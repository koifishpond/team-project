package entity.matchmaking;

import java.time.LocalDate;
import java.util.Objects;

/** Stored profile. Its ID is also the owning account's ID in v1. */
public final class DiscoveryProfile {
    private final ProfileId profileId;
    private final long profileRevision;
    private final DiscoveryStatus discoveryStatus;
    private final ProfileFields fields;

    public DiscoveryProfile(ProfileId id, long revision, DiscoveryStatus status, ProfileFields fields) {
        profileId = Objects.requireNonNull(id);
        if (revision < 1) throw new IllegalArgumentException("Revision");
        profileRevision = revision;
        discoveryStatus = Objects.requireNonNull(status);
        this.fields = Objects.requireNonNull(fields);
    }
    public ProfileId getProfileId() { return profileId; }
    public long getProfileRevision() { return profileRevision; }
    public DiscoveryStatus getDiscoveryStatus() { return discoveryStatus; }
    public ProfileFields getFields() { return fields; }
    public MatchingProfile matchingOn(LocalDate date) {
        return new MatchingProfile(profileId, profileRevision, discoveryStatus, fields.publicationErrors(date).isEmpty(),
                fields.ageOn(date), fields.getGender(), fields.getLocationArea(), fields.getInterestTags(),
                fields.getRelationshipIntent(), fields.getSmoking(), fields.getDrinking(), fields.getPreferences());
    }
    public PublicProfileSummary publicOn(LocalDate date) {
        return new PublicProfileSummary(profileId, profileRevision, fields.getDisplayName().orElseThrow(),
                fields.ageOn(date).orElseThrow(), fields.getPhotoIds().stream().findFirst(), fields.getBio(), fields.getInterestTags());
    }
}
