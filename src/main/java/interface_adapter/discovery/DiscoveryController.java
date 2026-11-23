package interface_adapter.discovery;

import use_case.discovery.*;
import java.util.*;

public final class DiscoveryController {
    private final NextPreviewInputBoundary input;
    public DiscoveryController(NextPreviewInputBoundary input) { this.input = Objects.requireNonNull(input); }
    public void next(UUID requestId, UUID sessionToken) { input.execute(new NextPreviewInputData(requestId, sessionToken)); }
    /** Malformed text is rejected here, before a valid Use Case invocation exists. */
    public void next(String requestId, String sessionToken) { next(parse(requestId), parse(sessionToken)); }
    private UUID parse(String value) {
        UUID parsed = UUID.fromString(Objects.requireNonNull(value));
        if (!parsed.toString().equalsIgnoreCase(value)) throw new IllegalArgumentException("Canonical UUID required");
        return parsed;
    }
}
