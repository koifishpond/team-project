package interface_adapter.discovery;

import entity.matchmaking.ProfileId;
import use_case.discovery.NextPreviewOutputData;
import java.util.*;

/** Formatted public values only; safe for a future View to render. */
public final class DiscoveryViewModel {
    private final UUID requestId;
    private final NextPreviewOutputData.Status status;
    private final Optional<Card> card;
    private final String message;
    public DiscoveryViewModel(UUID requestId, NextPreviewOutputData.Status status, Optional<Card> card, String message) {
        this.requestId = Objects.requireNonNull(requestId); this.status = Objects.requireNonNull(status);
        if ((status == NextPreviewOutputData.Status.SELECTED) != card.isPresent()) throw new IllegalArgumentException("Card payload");
        this.card = card; this.message = Objects.requireNonNull(message);
    }
    public UUID getRequestId() { return requestId; }
    public NextPreviewOutputData.Status getStatus() { return status; }
    public Optional<Card> getCard() { return card; }
    public String getMessage() { return message; }

    public static final class Card {
        private final ProfileId profileId;
        private final long profileRevision;
        private final String displayName;
        private final int ageYears;
        private final Optional<UUID> primaryPhotoId;
        private final Optional<String> distanceLabel, bioExcerpt;
        private final List<String> interestLabels;
        public Card(ProfileId id, long revision, String name, int age, Optional<UUID> photo,
                    Optional<String> distance, Optional<String> bio, List<String> interests) {
            profileId = Objects.requireNonNull(id); profileRevision = revision; displayName = Objects.requireNonNull(name); ageYears = age;
            primaryPhotoId = Objects.requireNonNull(photo); distanceLabel = Objects.requireNonNull(distance);
            bioExcerpt = Objects.requireNonNull(bio); interestLabels = List.copyOf(interests);
        }
        public ProfileId getProfileId() { return profileId; }
        public long getProfileRevision() { return profileRevision; }
        public String getDisplayName() { return displayName; }
        public int getAgeYears() { return ageYears; }
        public Optional<UUID> getPrimaryPhotoId() { return primaryPhotoId; }
        public Optional<String> getDistanceLabel() { return distanceLabel; }
        public Optional<String> getBioExcerpt() { return bioExcerpt; }
        public List<String> getInterestLabels() { return interestLabels; }
    }
}
