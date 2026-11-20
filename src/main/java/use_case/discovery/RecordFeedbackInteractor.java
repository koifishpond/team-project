package use_case.discovery;

import java.util.Objects;
import java.util.UUID;

/** Headless feedback use case. Selection never invokes this write boundary. */
public final class RecordFeedbackInteractor {
    private final FeedbackDataAccessInterface feedback;

    public RecordFeedbackInteractor(FeedbackDataAccessInterface feedback) {
        this.feedback = Objects.requireNonNull(feedback);
    }

    public FeedbackResult execute(UUID sessionToken, FeedbackEvent event) {
        Objects.requireNonNull(sessionToken);
        Objects.requireNonNull(event);
        try {
            return feedback.applyFeedback(sessionToken, event);
        } catch (DataAccessException failure) {
            return FeedbackResult.RETRYABLE_FAILURE;
        }
    }
}
