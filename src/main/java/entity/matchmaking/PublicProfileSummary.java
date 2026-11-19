package entity.matchmaking;

import java.util.*;

/** Public source for a preview; no private profile fields. */
public final class PublicProfileSummary {
    private final ProfileId profileId;
    private final long profileRevision;
    private final String displayName;
    private final int ageYears;
    private final Optional<UUID> primaryPhotoId;
    private final Optional<String> bio;
    private final Optional<Set<InterestTag>> interestTags;

    public PublicProfileSummary(ProfileId id, long revision, String name, int age, Optional<UUID> photo,
                                Optional<String> bio, Optional<Set<InterestTag>> interests) {
        profileId = Objects.requireNonNull(id);
        if (revision < 1 || age < 18) throw new IllegalArgumentException("Revision or adult age");
        Map<String, FieldError> errors = new LinkedHashMap<>();
        displayName = ProfileFields.normalizeText(Objects.requireNonNull(name), 40, false, "displayName", errors);
        this.bio = Optional.ofNullable(ProfileFields.normalizeText(Objects.requireNonNull(bio).orElse(null), 500, true, "bio", errors));
        if (displayName == null) errors.put("displayName", FieldError.REQUIRED);
        if (!errors.isEmpty()) throw new ProfileValidationException(errors);
        profileRevision = revision; ageYears = age;
        primaryPhotoId = Objects.requireNonNull(photo);
        interestTags = Objects.requireNonNull(interests).map(Set::copyOf);
        if (interestTags.isPresent() && interestTags.get().size() > 10) throw new IllegalArgumentException("Interests");
    }
    public ProfileId getProfileId() { return profileId; }
    public long getProfileRevision() { return profileRevision; }
    public String getDisplayName() { return displayName; }
    public int getAgeYears() { return ageYears; }
    public Optional<UUID> getPrimaryPhotoId() { return primaryPhotoId; }
    public Optional<String> getBio() { return bio; }
    public Optional<Set<InterestTag>> getInterestTags() { return interestTags; }
}
