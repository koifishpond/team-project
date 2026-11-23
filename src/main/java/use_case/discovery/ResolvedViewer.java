package use_case.discovery;

import entity.matchmaking.*;
import java.time.*;
import java.util.*;

public final class ResolvedViewer {
    private final ProfileId viewerId;
    private final UUID loginSessionId;
    private final UUID discoverySessionId;
    public ResolvedViewer(ProfileId viewerId, UUID loginSessionId, UUID discoverySessionId) {

        this.viewerId = Objects.requireNonNull(viewerId);
        this.loginSessionId = Objects.requireNonNull(loginSessionId);
        this.discoverySessionId = Objects.requireNonNull(discoverySessionId);
    }
    public ProfileId getViewerId() { return viewerId; }
    public UUID getLoginSessionId() { return loginSessionId; }
    public UUID getDiscoverySessionId() { return discoverySessionId; }
}
