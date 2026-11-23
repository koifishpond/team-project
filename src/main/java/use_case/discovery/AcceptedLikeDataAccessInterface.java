package use_case.discovery;

/** Backend-only transaction for replay of an already-authenticated acceptance. */
public interface AcceptedLikeDataAccessInterface {
    FeedbackResult replayAcceptedLike(BackendAcceptedLike accepted);
}
