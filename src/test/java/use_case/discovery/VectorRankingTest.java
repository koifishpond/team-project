package use_case.discovery;

import entity.matchmaking.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

final class VectorRankingTest {
    private static final double TOLERANCE = 1e-12;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);
    private final ViewerScorer scorer = new ViewerScorer();

    @Test void independentMaskedDotProductIncludesSignedTermsAndOneHotBudget() {
        double[] preference = new double[DiscoveryFeatures.dimension()];
        double[] attributes = new double[preference.length];
        BitSet known = new BitSet();
        int hiking = index("interest:HIKING"), books = index("interest:BOOKS");
        int age28 = index("age:28"), age29 = index("age:29"), drinking = index("drinking:NEVER");
        preference[hiking] = 0.6;
        preference[books] = 0.2;
        preference[age28] = 0.4;
        preference[age29] = 0.8;
        preference[drinking] = -0.4;
        attributes[hiking] = 1;
        attributes[books] = -1;
        attributes[age28] = 1;
        known.set(hiking);
        known.set(books);
        known.set(age28);
        known.set(age29);
        // Dot = 0.6 - 0.2 + 0.4. Viewer budget = 0.8 interests + 0.8 age + 0.4 drinking.
        assertEquals(0.70, scorer.score(new ViewerPreferenceVector(1, preference),
                new CandidateAttributeVector(1, attributes, known)), TOLERANCE);
    }

    @Test void vectorsCopyInputsAndRejectInvalidShapeAndIncompatibleSchemas() {
        int dimension = DiscoveryFeatures.dimension();
        double[] values = new double[dimension];
        values[0] = 0.8;
        BitSet known = new BitSet();
        known.set(0);
        ViewerPreferenceVector preference = new ViewerPreferenceVector(1, values);
        CandidateAttributeVector candidate = new CandidateAttributeVector(1, values, known);
        values[0] = 0;
        known.clear();
        assertEquals(0.8, preference.affinityAt(0));
        assertEquals(0.8, candidate.valueAt(0));
        assertTrue(candidate.isKnown(0));
        assertThrows(IllegalArgumentException.class, () -> scorer.score(preference, new CandidateAttributeVector(2, values, known)));
        assertThrows(IllegalArgumentException.class, () -> scorer.score(new ViewerPreferenceVector(1, new double[2]), candidate));
        assertThrows(IllegalArgumentException.class, () -> new ViewerPreferenceVector(1, new double[] {Double.NaN}));
        assertThrows(IllegalArgumentException.class, () -> new CandidateAttributeVector(1, new double[] {1}, known));
        assertThrows(IllegalArgumentException.class, () -> new CandidateAttributeVector(0, values, known));
    }

    @Test void unknownInterestsAreNeutralButKnownEmptyCanDisagreeWithAPreference() {
        ProfileFields.Input viewer = profileInput("Alex");
        viewer.preferredInterestTags = List.of("HIKING");
        ViewerPreferenceVector preference = preferences(viewer);
        ProfileFields.Input candidate = profileInput("Taylor");
        int hiking = index("interest:HIKING");
        CandidateAttributeVector unknown = attributes(candidate, OptionalInt.empty());
        assertFalse(unknown.isKnown(hiking));
        assertEquals(0.5, scorer.score(preference, unknown));
        candidate.interestTags = List.of();
        CandidateAttributeVector empty = attributes(candidate, OptionalInt.empty());
        assertTrue(empty.isKnown(hiking));
        assertEquals(-1, empty.valueAt(hiking));
        assertEquals(0.25, scorer.score(preference, empty));
        candidate.interestTags = List.of("HIKING");
        double match = scorer.score(preference, attributes(candidate, OptionalInt.empty()));
        assertEquals(0.75, match);
        candidate.interestTags = List.of("ART", "HIKING", "BOOKS");
        assertEquals(match, scorer.score(preference, attributes(candidate, OptionalInt.empty())));
        assertEquals(0.5, scorer.score(preferences(profileInput("No settings")), unknown));
    }

    @Test void ageDistanceIntentAndBothLifestyleAttributesUseDistinctCoordinates() {
        ProfileFields.Input candidate = profileInput("Taylor");
        candidate.relationshipIntent = "LONG_TERM";
        candidate.smoking = "NEVER";
        candidate.drinking = "SOMETIMES";
        CandidateAttributeVector vector = attributes(candidate, OptionalInt.of(10001));
        assertEquals(228, vector.dimension());
        for (String feature : Set.of("age:28", "distance:15", "intent:LONG_TERM", "smoking:NEVER", "drinking:SOMETIMES")) {
            assertTrue(vector.isKnown(index(feature)), feature);
            assertEquals(1, vector.valueAt(index(feature)), feature);
        }
        assertEquals(0, vector.valueAt(index("smoking:SOMETIMES")));
        assertTrue(vector.isKnown(index("smoking:SOMETIMES")));
        assertFalse(attributes(profileInput("Missing lifestyle"), OptionalInt.empty()).isKnown(index("smoking:NEVER")));

        ProfileFields.Input viewer = profileInput("Alex");
        viewer.minYears = 25;
        viewer.maxYears = 30;
        viewer.ageMode = "SOFT";
        viewer.maxKm = 10;
        viewer.distanceMode = "SOFT";
        ViewerPreferenceVector preference = preferences(viewer);
        assertEquals(0.5, preference.affinityAt(index("age:28")));
        assertEquals(0.45, preference.affinityAt(index("age:31")), TOLERANCE);
        assertEquals(0.375, preference.affinityAt(index("distance:5")), TOLERANCE);
        assertEquals(0, preference.affinityAt(index("distance:15")));
        assertEquals(0, preference.affinityAt(index("intent:LONG_TERM")));
    }

    @Test void directionUsesViewerPreferencesAndCandidateOwnAttributes() {
        ProfileFields.Input alex = profileInput("Alex"), taylor = profileInput("Taylor"), morgan = profileInput("Morgan");
        alex.interestTags = List.of("COOKING");
        alex.preferredInterestTags = List.of("ART");
        taylor.interestTags = List.of("ART");
        taylor.preferredInterestTags = List.of("HIKING");
        morgan.interestTags = List.of("BOOKS");
        CandidateAttributeVector taylorAttributes = attributes(taylor, OptionalInt.empty());
        CandidateAttributeVector morganAttributes = attributes(morgan, OptionalInt.empty());
        assertEquals(0.75, scorer.score(preferences(alex), taylorAttributes));
        assertEquals(0.25, scorer.score(preferences(taylor), attributes(alex, OptionalInt.empty())));
        assertTrue(scorer.score(preferences(alex), taylorAttributes) > scorer.score(preferences(alex), morganAttributes));
        alex.preferredInterestTags = List.of("BOOKS");
        assertTrue(scorer.score(preferences(alex), taylorAttributes) < scorer.score(preferences(alex), morganAttributes));
        taylor.preferredInterestTags = List.of("ART");
        assertEquals(scorer.score(preferences(alex), taylorAttributes),
                scorer.score(preferences(alex), attributes(taylor, OptionalInt.empty())));
    }

    @Test void learnedIntentAndLifestyleCanRankOtherwiseIdenticalProfilesAndAffinitiesStayBounded() {
        ProfileFields.Input viewer = profileInput("Alex");
        viewer.preferredInterestTags = List.of("HIKING");
        MatchingProfile alex = profile(viewer);
        ViewerPreferenceVector learned = DiscoveryFeatures.preferences(alex.getPreferences(),
                Map.of("interest:HIKING", 2.0, "intent:LONG_TERM", 0.6, "smoking:REGULARLY", -2.0));
        assertEquals(1, learned.affinityAt(index("interest:HIKING")));
        assertEquals(-1, learned.affinityAt(index("smoking:REGULARLY")));
        assertEquals(0.5, preferences(viewer).affinityAt(index("interest:HIKING")));
        ProfileFields.Input candidate = profileInput("Taylor");
        candidate.interestTags = List.of("HIKING");
        double baseline = scorer.score(learned, attributes(candidate, OptionalInt.empty()));
        candidate.relationshipIntent = "LONG_TERM";
        assertTrue(scorer.score(learned, attributes(candidate, OptionalInt.empty())) > baseline);
        candidate.smoking = "REGULARLY";
        assertTrue(scorer.score(learned, attributes(candidate, OptionalInt.empty())) < baseline);
    }

    private static int index(String key) { return DiscoveryFeatures.indexOf(key); }

    private static ProfileFields.Input profileInput(String name) {
        ProfileFields.Input input = new ProfileFields.Input();
        input.displayName = name;
        input.audience = "ANY";
        input.dateOfBirth = TODAY.minusYears(28);
        return input;
    }

    private static MatchingProfile profile(ProfileFields.Input input) {
        return new DiscoveryProfile(new ProfileId(100000001), 1, DiscoveryStatus.ACTIVE,
                ProfileFields.from(input, TODAY)).matchingOn(TODAY);
    }

    private static ViewerPreferenceVector preferences(ProfileFields.Input input) {
        return DiscoveryFeatures.preferences(profile(input).getPreferences());
    }

    private static CandidateAttributeVector attributes(ProfileFields.Input input, OptionalInt distance) {
        return DiscoveryFeatures.candidate(profile(input), distance);
    }
}
