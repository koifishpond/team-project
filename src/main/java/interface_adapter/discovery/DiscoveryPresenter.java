package interface_adapter.discovery;

import entity.matchmaking.InterestTag;
import use_case.discovery.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class DiscoveryPresenter implements NextPreviewOutputBoundary {
    private final Consumer<DiscoveryViewModel> view;
    public DiscoveryPresenter(Consumer<DiscoveryViewModel> view) { this.view = Objects.requireNonNull(view); }
    @Override public void present(NextPreviewOutputData data) {
        Optional<DiscoveryViewModel.Card> card = data.getSelectedCandidate().map(c -> new DiscoveryViewModel.Card(
                c.getProfileId(), c.getProfileRevision(), c.getDisplayName(), c.getAgeYears(), c.getPrimaryPhotoId(),
                c.getDistanceMetres().isPresent() ? Optional.of(distanceLabel(c.getDistanceMetres().getAsInt())) : Optional.empty(),
                c.getBioExcerpt(), c.getPreviewInterestTags().stream().map(InterestTag::getLabel).collect(Collectors.toList())));
        String message;
        switch (data.getStatus()) {
            case SELECTED: message = "Selected profile"; break;
            case EMPTY: message = "No more profiles available right now. Refresh or edit preferences."; break;
            case SESSION_EXPIRED: message = "Sign in again to continue."; break;
            case VIEWER_UNAVAILABLE: message = "Complete and activate your discovery profile."; break;
            case RETRYABLE_FAILURE: message = "Temporarily unavailable. Retry with the same request ID."; break;
            default: throw new IllegalStateException("Unknown status");
        }
        view.accept(new DiscoveryViewModel(data.getRequestId(), data.getStatus(), card, message));
    }
    public static String distanceLabel(int metres) {
        if (metres < 0) throw new IllegalArgumentException("Distance");
        return metres == 0 ? "Same area" : "Within " + (5 * (long) Math.ceil(metres / 5000.0)) + " km";
    }
}
