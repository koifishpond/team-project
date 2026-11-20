package use_case.discovery;

import java.util.Objects;

/** Bound to the backend driver, never to the Explore Controller. */
public final class ReplayAcceptedLikeInteractor {
    private final AcceptedLikeDataAccessInterface acceptedLikes;

    public ReplayAcceptedLikeInteractor(AcceptedLikeDataAccessInterface acceptedLikes) {
        this.acceptedLikes = Objects.requireNonNull(acceptedLikes);
    }

    public FeedbackResult execute(BackendAcceptedLike accepted) {
        try {
            return acceptedLikes.replayAcceptedLike(Objects.requireNonNull(accepted));
        } catch (DataAccessException failure) {
            return FeedbackResult.RETRYABLE_FAILURE;
        }
    }
}
