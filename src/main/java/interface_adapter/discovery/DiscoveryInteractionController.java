package interface_adapter.discovery;

import use_case.discovery.*;
import java.util.Objects;
import java.util.UUID;

public final class DiscoveryInteractionController {
    private final RecordInteractionInputBoundary feedback;
    private final OpenProfileInputBoundary profiles;

    public DiscoveryInteractionController(RecordInteractionInputBoundary feedback, OpenProfileInputBoundary profiles) {
        this.feedback = Objects.requireNonNull(feedback);
        this.profiles = Objects.requireNonNull(profiles);
    }

    public void interact(UUID token, FeedbackEvent event) {
        feedback.execute(new InteractionInputData(token, event));
    }

    public void open(UUID requestId, UUID token, UUID exposureId) {
        profiles.execute(new OpenProfileInputData(requestId, token, exposureId));
    }
}
