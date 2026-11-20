package use_case.discovery;

import entity.matchmaking.*;
import java.time.*;
import java.util.*;

public final class NextPreviewInputData {
    private final UUID requestId;
    private final UUID sessionToken;
    public NextPreviewInputData(UUID requestId, UUID sessionToken) {

        this.requestId = Objects.requireNonNull(requestId);
        this.sessionToken = Objects.requireNonNull(sessionToken);
    }
    public UUID getRequestId() { return requestId; }
    public UUID getSessionToken() { return sessionToken; }
}
