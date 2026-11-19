package use_case.discovery;

import entity.matchmaking.*;
import java.util.*;

/** Fixed v1 viewer-direction kernel. Candidate preferences never enter the score. */
public final class ViewerScorer {
    public double score(MatchingProfile viewer, MatchingProfile candidate, OptionalInt distanceMetres) {
        if (distanceMetres.isPresent() && distanceMetres.getAsInt() < 0) throw new IllegalArgumentException("Distance");
        PartnerPreferences p = viewer.getPreferences();
        Set<InterestTag> preferred = p.getPreferredInterestTags();
        double interests = 0.5, distance = 0.5, age = 0.5;
        if (!preferred.isEmpty() && candidate.getInterestTags().isPresent()) {
            long common = candidate.getInterestTags().get().stream().filter(preferred::contains).count();
            interests = common / (double) preferred.size();
        }
        if (p.getDistancePreference().isPresent() && distanceMetres.isPresent())
            distance = Math.max(0, 1 - distanceMetres.getAsInt() / (p.getDistancePreference().get().getMaxKm() * 1000.0));
        if (p.getAgePreference().isPresent() && candidate.getAgeYears().isPresent()) {
            AgePreference range = p.getAgePreference().get();
            int years = candidate.getAgeYears().getAsInt();
            int outside = Math.max(0, Math.max(range.getMinYears() - years, years - range.getMaxYears()));
            age = Math.max(0, 1 - outside / 10.0);
        }
        return 0.60 * interests + 0.25 * distance + 0.15 * age;
    }
}
