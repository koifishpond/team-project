package use_case.discovery;

import java.util.Objects;
import java.util.UUID;

public final class InteractionInputData {
    private final UUID sessionToken;
    private final FeedbackEvent event;

    public InteractionInputData(UUID sessionToken, FeedbackEvent event) {
        this.sessionToken = Objects.requireNonNull(sessionToken);
        this.event = Objects.requireNonNull(event);
    }

    public UUID getSessionToken() { return sessionToken; }
    public FeedbackEvent getEvent() { return event; }
}
