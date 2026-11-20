package use_case.discovery;

import java.util.Objects;

public final class RecordFeedbackInteractor implements RecordInteractionInputBoundary {
    private final FeedbackDataAccessInterface feedback;
    private final InteractionOutputBoundary output;

    public RecordFeedbackInteractor(FeedbackDataAccessInterface feedback, InteractionOutputBoundary output) {
        this.feedback = Objects.requireNonNull(feedback);
        this.output = Objects.requireNonNull(output);
    }

    @Override public void execute(InteractionInputData input) {
        FeedbackResult result;
        try {
            result = feedback.applyFeedback(input.getSessionToken(), input.getEvent());
        } catch (DataAccessException failure) {
            result = FeedbackResult.RETRYABLE_FAILURE;
        }
        output.present(new InteractionOutputData(input.getEvent(), result));
    }
}
