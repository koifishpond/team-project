package use_case.discovery;

/** Atomic fixture ingestion of an already-authenticated backend message. */
public interface MatchConfirmationDataAccessInterface {
    FeedbackResult recordMatch(BackendMatchConfirmation confirmation);
}
