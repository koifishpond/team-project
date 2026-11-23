package integration;

import data_access.discovery.HttpProfileAssessmentDataAccess;
import entity.matchmaking.ProfileAssessment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import use_case.discovery.*;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static use_case.discovery.AssessmentResult.Status.*;

final class ProfileAssessmentFlowTest {
    private static final String READY_JSON = "{\"status\":\"READY\",\"schemaVersion\":1,\"rubricVersion\":1,\"profileRevision\":3,"
            + "\"modelVersion\":\"fixture-v1\",\"effort\":0.75,\"personality\":\"INFP\"}";
    private final AssessmentInput input = new AssessmentInput(AssessmentInput.Fixture.DETAILED, 3);

    @Test void syntheticInputThroughTheHttpAdapterValidatesTheVersionedResult() {
        AssessmentResult result = assess((request, timeout) -> {
            assertEquals("POST", request.method());
            assertEquals("Bearer unit-test-credential", request.headers().firstValue("Authorization").orElseThrow());
            assertEquals(Duration.ofMillis(100), timeout);
            String payload = body(request.bodyPublisher().orElseThrow());
            assertTrue(payload.contains("\"synthetic\":true"));
            assertTrue(payload.contains("clarinet"));
            assertTrue(payload.contains("\"profileRevision\":3"));
            assertFalse(payload.contains("profileId"));
            assertFalse(payload.contains("dateOfBirth"));
            assertFalse(payload.contains("credential"));
            return new HttpProfileAssessmentDataAccess.Response(200, READY_JSON);
        });
        assertEquals(READY, result.status());
        ProfileAssessment value = result.assessment().orElseThrow();
        assertEquals(0.75, value.effort());
        assertEquals(ProfileAssessment.Personality.INFP, value.personality());
        assertEquals("fixture-v1", value.modelVersion());
        assertEquals(3, value.profileRevision());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{bad", "WRONG_SCHEMA", "WRONG_RUBRIC", "WRONG_REVISION", "BAD_EFFORT", "BAD_PERSONALITY", "TRAILING", "DUPLICATE_KEY"})
    void malformedOrMismatchedResultsFallBackToNoAssessment(String variant) {
        String body;
        switch (variant) {
            case "WRONG_SCHEMA": body = READY_JSON.replace("\"schemaVersion\":1", "\"schemaVersion\":2"); break;
            case "WRONG_RUBRIC": body = READY_JSON.replace("\"rubricVersion\":1", "\"rubricVersion\":2"); break;
            case "WRONG_REVISION": body = READY_JSON.replace("\"profileRevision\":3", "\"profileRevision\":4"); break;
            case "BAD_EFFORT": body = READY_JSON.replace("0.75", "1.25"); break;
            case "BAD_PERSONALITY": body = READY_JSON.replace("INFP", "invented"); break;
            case "TRAILING": body = READY_JSON + "{}"; break;
            case "DUPLICATE_KEY": body = READY_JSON.replace("\"effort\":0.75", "\"effort\":-2,\"effort\":0.75"); break;
            default: body = variant;
        }
        AssessmentResult result = assess((request, timeout) -> new HttpProfileAssessmentDataAccess.Response(200, body));
        assertEquals(INVALID_RESPONSE, result.status()); assertTrue(result.assessment().isEmpty());
    }

    @Test void timeoutTransportFailureServerErrorAndMissingConfigurationHaveNeutralFallbacks() {
        assertEquals(TIMEOUT, assess((request, timeout) -> { throw new TimeoutException(); }).status());
        assertEquals(TIMEOUT, assess((request, timeout) -> new HttpProfileAssessmentDataAccess.Response(504, "")).status());
        assertEquals(UNAVAILABLE, assess((request, timeout) -> { throw new IOException(); }).status());
        assertEquals(UNAVAILABLE, assess((request, timeout) -> new HttpProfileAssessmentDataAccess.Response(503, "")).status());
        assertEquals(NOT_CONFIGURED, new AssessProfileInteractor(HttpProfileAssessmentDataAccess.configured(Map.of())).execute(input).status());
        assertEquals(INSUFFICIENT_EVIDENCE, assess((request, timeout) -> new HttpProfileAssessmentDataAccess.Response(200,
                "{\"status\":\"INSUFFICIENT_EVIDENCE\",\"schemaVersion\":1,\"rubricVersion\":1,\"profileRevision\":3}")).status());
    }

    private AssessmentResult assess(HttpProfileAssessmentDataAccess.Transport transport) {
        return new AssessProfileInteractor(new HttpProfileAssessmentDataAccess(URI.create("https://assessment.example.invalid/evaluate"),
                "unit-test-credential", Duration.ofMillis(100), transport)).execute(input);
    }

    private static String body(java.net.http.HttpRequest.BodyPublisher publisher) throws IOException, InterruptedException {
        CompletableFuture<String> body = new CompletableFuture<>();
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        publisher.subscribe(new Flow.Subscriber<ByteBuffer>() {
            @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
            @Override public void onNext(ByteBuffer item) { byte[] chunk = new byte[item.remaining()]; item.get(chunk); bytes.writeBytes(chunk); }
            @Override public void onError(Throwable error) { body.completeExceptionally(error); }
            @Override public void onComplete() { body.complete(bytes.toString(StandardCharsets.UTF_8)); }
        });
        try { return body.get(1, TimeUnit.SECONDS); }
        catch (ExecutionException | TimeoutException error) { throw new IOException(error); }
    }
}
