package use_case.discovery;

import entity.matchmaking.*;
import java.util.OptionalInt;

/** Discovery-specific mutual hard rules. Scoring cannot bypass this policy. */
public final class EligibilityPolicy {
    public boolean available(MatchingProfile profile) {
        return profile.getDiscoveryStatus() == DiscoveryStatus.ACTIVE && profile.isPublicationValid();
    }
    public OptionalInt distance(MatchingProfile a, MatchingProfile b) {
        return a.getLocationArea().isPresent() && b.getLocationArea().isPresent()
                ? OptionalInt.of(GeoDistance.between(a.getLocationArea().get(), b.getLocationArea().get())) : OptionalInt.empty();
    }
    public boolean eligible(MatchingProfile viewer, MatchingProfile candidate, PairState pair) {
        return eligible(viewer, candidate, pair, distance(viewer, candidate));
    }
    /** Supplied metres make exact boundary cases independently testable. */
    public boolean eligible(MatchingProfile viewer, MatchingProfile candidate, PairState pair, OptionalInt metres) {
        if (metres.isPresent() && metres.getAsInt() < 0) throw new IllegalArgumentException("Distance");
        return !viewer.getProfileId().equals(candidate.getProfileId()) && available(viewer) && available(candidate)
                && !pair.excludes() && accepts(viewer, candidate, metres) && accepts(candidate, viewer, metres);
    }
    private boolean accepts(MatchingProfile owner, MatchingProfile other, OptionalInt metres) {
        PartnerPreferences p = owner.getPreferences();
        if (p.getAudience() == AudienceMode.UNSET) return false;
        if (p.getAudience() == AudienceMode.SELECTED
                && (!other.getGender().isPresent() || !p.getAcceptedGenders().contains(other.getGender().get()))) return false;
        if (p.getAgePreference().isPresent()) {
            AgePreference age = p.getAgePreference().get();
            if (age.getMode() == PreferenceMode.HARD && (!other.getAgeYears().isPresent()
                    || other.getAgeYears().getAsInt() < age.getMinYears() || other.getAgeYears().getAsInt() > age.getMaxYears())) return false;
        }
        if (p.getDistancePreference().isPresent()) {
            DistancePreference distance = p.getDistancePreference().get();
            if (distance.getMode() == PreferenceMode.HARD
                    && (!metres.isPresent() || metres.getAsInt() > distance.getMaxKm() * 1000)) return false;
        }
        return true;
    }
}
