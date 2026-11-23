package use_case.discovery;

import java.util.UUID;

public interface FeedbackDataAccessInterface {
    /** Atomically check the session/exposure, deduplicate and update learned state. */
    FeedbackResult applyFeedback(UUID sessionToken, FeedbackEvent event);
}
