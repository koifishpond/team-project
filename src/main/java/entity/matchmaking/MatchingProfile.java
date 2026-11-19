package entity.matchmaking;

import java.util.*;

/** Matching projection only; it cannot expose the raw birth date or account credentials. */
public final class MatchingProfile {
    private final ProfileId profileId;
    private final long profileRevision;
    private final DiscoveryStatus discoveryStatus;
    private final boolean publicationValid;
    private final OptionalInt ageYears;
    private final Optional<Gender> gender;
    private final Optional<LocationArea> locationArea;
    private final Optional<Set<InterestTag>> interestTags;
    private final PartnerPreferences preferences;

    public MatchingProfile(ProfileId id, long revision, DiscoveryStatus status, boolean valid, OptionalInt age,
                           Optional<Gender> gender, Optional<LocationArea> location,
                           Optional<Set<InterestTag>> interests, PartnerPreferences preferences) {
        profileId = Objects.requireNonNull(id);
        if (revision < 1 || (age.isPresent() && age.getAsInt() < 0)) throw new IllegalArgumentException("Revision or age");
        profileRevision = revision; discoveryStatus = Objects.requireNonNull(status); publicationValid = valid;
        ageYears = Objects.requireNonNull(age); this.gender = Objects.requireNonNull(gender);
        locationArea = Objects.requireNonNull(location);
        interestTags = Objects.requireNonNull(interests).map(Set::copyOf);
        if (interestTags.isPresent() && interestTags.get().size() > 10) throw new IllegalArgumentException("Interests");
        this.preferences = Objects.requireNonNull(preferences);
        if (valid && (!age.isPresent() || age.getAsInt() < 18 || preferences.getAudience() == AudienceMode.UNSET))
            throw new IllegalArgumentException("Publication projection");
    }
    public ProfileId getProfileId() { return profileId; }
    public long getProfileRevision() { return profileRevision; }
    public DiscoveryStatus getDiscoveryStatus() { return discoveryStatus; }
    public boolean isPublicationValid() { return publicationValid; }
    public OptionalInt getAgeYears() { return ageYears; }
    public Optional<Gender> getGender() { return gender; }
    public Optional<LocationArea> getLocationArea() { return locationArea; }
    public Optional<Set<InterestTag>> getInterestTags() { return interestTags; }
    public PartnerPreferences getPreferences() { return preferences; }
}
