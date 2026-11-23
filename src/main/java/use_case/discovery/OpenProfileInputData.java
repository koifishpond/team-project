package use_case.discovery;

import java.util.Objects;
import java.util.UUID;

public final class OpenProfileInputData {
    private final UUID requestId;
    private final UUID sessionToken;
    private final UUID exposureId;

    public OpenProfileInputData(UUID requestId, UUID sessionToken, UUID exposureId) {
        this.requestId = Objects.requireNonNull(requestId);
        this.sessionToken = Objects.requireNonNull(sessionToken);
        this.exposureId = Objects.requireNonNull(exposureId);
    }

    public UUID getRequestId() { return requestId; }
    public UUID getSessionToken() { return sessionToken; }
    public UUID getExposureId() { return exposureId; }
}
