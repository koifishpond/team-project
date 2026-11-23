package entity.matchmaking;

import java.util.*;
public final class PartnerPreferences {
    private final AudienceMode audience;
    private final Set<Gender> acceptedGenders;
    private final Set<InterestTag> preferredInterestTags;
    private final AgePreference agePreference;
    private final DistancePreference distancePreference;
    public PartnerPreferences(AudienceMode audience, Set<Gender> genders, Set<InterestTag> interests,
                              AgePreference age, DistancePreference distance) {
        this.audience = Objects.requireNonNull(audience);
        acceptedGenders = Set.copyOf(genders); preferredInterestTags = Set.copyOf(interests);
        if ((audience == AudienceMode.SELECTED) == acceptedGenders.isEmpty() || preferredInterestTags.size() > 10)
            throw new IllegalArgumentException("Audience or interests");
        agePreference = age; distancePreference = distance;
    }
    public AudienceMode getAudience() { return audience; }
    public Set<Gender> getAcceptedGenders() { return acceptedGenders; }
    public Set<InterestTag> getPreferredInterestTags() { return preferredInterestTags; }
    public Optional<AgePreference> getAgePreference() { return Optional.ofNullable(agePreference); }
    public Optional<DistancePreference> getDistancePreference() { return Optional.ofNullable(distancePreference); }
    @Override public boolean equals(Object o) {
        if (!(o instanceof PartnerPreferences)) return false;
        PartnerPreferences a = (PartnerPreferences) o;
        return audience == a.audience && acceptedGenders.equals(a.acceptedGenders) && preferredInterestTags.equals(a.preferredInterestTags)
            && Objects.equals(agePreference, a.agePreference) && Objects.equals(distancePreference, a.distancePreference);
    }
    @Override public int hashCode() { return Objects.hash(audience, acceptedGenders, preferredInterestTags, agePreference, distancePreference); }
}
