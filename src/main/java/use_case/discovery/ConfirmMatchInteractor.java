package use_case.discovery;

import java.util.Objects;

/** Bound to the backend driver, never to the Explore Controller. */
public final class ConfirmMatchInteractor {
    private final MatchConfirmationDataAccessInterface matches;

    public ConfirmMatchInteractor(MatchConfirmationDataAccessInterface matches) { this.matches = Objects.requireNonNull(matches); }

    public FeedbackResult execute(BackendMatchConfirmation confirmation) {
        try {
            return matches.recordMatch(Objects.requireNonNull(confirmation));
        } catch (DataAccessException failure) {
            return FeedbackResult.RETRYABLE_FAILURE;
        }
    }
}
