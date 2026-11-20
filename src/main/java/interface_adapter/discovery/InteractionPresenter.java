package interface_adapter.discovery;

import use_case.discovery.InteractionOutputBoundary;
import use_case.discovery.InteractionOutputData;
import java.util.Objects;
import java.util.function.Consumer;

/** Receipts contain only correlated, public display state. No matching or storage state is shared. */
public final class InteractionPresenter implements InteractionOutputBoundary {
    private final Consumer<InteractionViewModel> view;

    public InteractionPresenter(Consumer<InteractionViewModel> view) { this.view = Objects.requireNonNull(view); }

    @Override public void present(InteractionOutputData output) {
        String message;
        switch (output.getStatus()) {
            case ACCEPTED: message = "Interaction recorded."; break;
            case ALREADY_APPLIED: message = "Interaction was already recorded."; break;
            case REJECTED:
                message = output.getRejection().orElseThrow() == InteractionOutputData.Rejection.PREVIEW_UNAVAILABLE
                        ? "This preview is no longer available." : "This interaction was rejected.";
                break;
            case SESSION_EXPIRED: message = "Sign in again to continue."; break;
            case RETRYABLE_FAILURE: message = "Retry using the same event ID."; break;
            default: throw new IllegalArgumentException("Unknown feedback status");
        }
        view.accept(new InteractionViewModel(output, message));
    }
}
