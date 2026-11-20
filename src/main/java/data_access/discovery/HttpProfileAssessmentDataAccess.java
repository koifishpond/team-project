package data_access.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import entity.matchmaking.ProfileAssessment;
import use_case.discovery.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** JSON gateway to a configured assessment service. The service mediates whichever model is adopted. */
public final class HttpProfileAssessmentDataAccess implements ProfileAssessmentDataAccessInterface {
    private static final int MAX_RESPONSE_CHARS = 16_384;

    @FunctionalInterface
    public interface Transport {
        Response post(HttpRequest request, Duration timeout) throws IOException, InterruptedException, TimeoutException;
    }

    public static final class Response {
        private final int status;
        private final String body;

        public Response(int status, String body) {
            this.status = status;
            this.body = Objects.requireNonNull(body);
        }
    }

    private final URI endpoint;
    private final String token;
    private final Duration timeout;
    private final Transport transport;
    private final ObjectMapper json = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
            DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);

    public HttpProfileAssessmentDataAccess(URI endpoint, String token, Duration timeout, Transport transport) {
        this.endpoint = Objects.requireNonNull(endpoint);
        if (!"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null || endpoint.getUserInfo() != null
                || endpoint.getFragment() != null || endpoint.getRawQuery() != null) {
            throw new IllegalArgumentException("Assessment endpoint must be an HTTPS URL without credentials or query parameters");
        }
        this.token = Objects.requireNonNull(token);
        if (token.isBlank() || !token.matches("[!-~]+")) throw new IllegalArgumentException("Invalid assessment credential");
        this.timeout = Objects.requireNonNull(timeout);
        if (timeout.isNegative() || timeout.toMillis() < 1) throw new IllegalArgumentException("Invalid provider timeout");
        this.transport = Objects.requireNonNull(transport);
    }

    public static ProfileAssessmentDataAccessInterface configured(Map<String, String> environment) {
        String endpoint = environment.get("MATCHMAKING_ASSESSMENT_URL");
        String token = environment.get("MATCHMAKING_ASSESSMENT_TOKEN");
        if (endpoint == null || endpoint.isBlank() || token == null || token.isBlank()) {
            return input -> AssessmentResult.absent(AssessmentResult.Status.NOT_CONFIGURED);
        }
        try {
            Duration timeout = Duration.ofSeconds(5);
            HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).followRedirects(HttpClient.Redirect.NEVER).build();
            return new HttpProfileAssessmentDataAccess(URI.create(endpoint), token, timeout, (request, limit) -> {
                CompletableFuture<HttpResponse<String>> pending = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
                try {
                    HttpResponse<String> response = pending.get(limit.toMillis(), TimeUnit.MILLISECONDS);
                    return new Response(response.statusCode(), response.body());
                } catch (TimeoutException | InterruptedException e) {
                    pending.cancel(true);
                    throw e;
                } catch (ExecutionException e) {
                    if (e.getCause() instanceof HttpTimeoutException) throw new TimeoutException("Assessment timed out");
                    throw new IOException("Assessment transport unavailable", e);
                }
            });
        } catch (IllegalArgumentException e) {
            return input -> AssessmentResult.absent(AssessmentResult.Status.NOT_CONFIGURED);
        }
    }

    @Override public AssessmentResult assess(AssessmentInput input) {
        try {
            String payload = json.writeValueAsString(Map.of("schemaVersion", AssessmentInput.SCHEMA_VERSION,
                    "rubricVersion", AssessmentInput.RUBRIC_VERSION, "profileRevision", input.revision(),
                    "bio", input.biography(), "interests", input.interests(), "synthetic", true));
            HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(timeout).header("Content-Type", "application/json")
                    .header("Accept", "application/json").header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(payload)).build();
            Response response = transport.post(request, timeout);
            if (response.status == 408 || response.status == 504) return AssessmentResult.absent(AssessmentResult.Status.TIMEOUT);
            if (response.status < 200 || response.status >= 300) return AssessmentResult.absent(AssessmentResult.Status.UNAVAILABLE);
            if (response.body.length() > MAX_RESPONSE_CHARS) return AssessmentResult.absent(AssessmentResult.Status.INVALID_RESPONSE);
            return decode(json.readTree(response.body), input);
        } catch (TimeoutException | HttpTimeoutException e) {
            return AssessmentResult.absent(AssessmentResult.Status.TIMEOUT);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return AssessmentResult.absent(AssessmentResult.Status.INVALID_RESPONSE);
        } catch (IOException e) {
            return AssessmentResult.absent(AssessmentResult.Status.UNAVAILABLE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return AssessmentResult.absent(AssessmentResult.Status.UNAVAILABLE);
        }
    }

    private AssessmentResult decode(JsonNode root, AssessmentInput input) {
        if (root == null || !root.isObject() || !root.path("schemaVersion").isIntegralNumber()
                || !root.path("rubricVersion").isIntegralNumber() || !root.path("profileRevision").isIntegralNumber()
                || !root.path("schemaVersion").canConvertToInt() || !root.path("rubricVersion").canConvertToInt()
                || !root.path("profileRevision").canConvertToLong()
                || root.path("schemaVersion").intValue() != AssessmentInput.SCHEMA_VERSION
                || root.path("rubricVersion").intValue() != AssessmentInput.RUBRIC_VERSION
                || root.path("profileRevision").longValue() != input.revision()) {
            return AssessmentResult.absent(AssessmentResult.Status.INVALID_RESPONSE);
        }
        String status = root.path("status").asText();
        if ("INSUFFICIENT_EVIDENCE".equals(status)) return AssessmentResult.absent(AssessmentResult.Status.INSUFFICIENT_EVIDENCE);
        if (!"READY".equals(status) || !root.path("effort").isNumber() || !root.path("personality").isTextual()
                || !root.path("modelVersion").isTextual()) return AssessmentResult.absent(AssessmentResult.Status.INVALID_RESPONSE);
        return AssessmentResult.ready(new ProfileAssessment(AssessmentInput.SCHEMA_VERSION, AssessmentInput.RUBRIC_VERSION,
                input.revision(), root.path("modelVersion").textValue(), root.path("effort").doubleValue(),
                ProfileAssessment.Personality.valueOf(root.path("personality").textValue())));
    }
}
