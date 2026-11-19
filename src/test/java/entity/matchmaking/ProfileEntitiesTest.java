package entity.matchmaking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfileEntitiesTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);
    private ProfileFields.Input minimum() {
        ProfileFields.Input in = new ProfileFields.Input();
        in.displayName = "Alex"; in.dateOfBirth = LocalDate.of(2000, 1, 1); in.audience = "ANY";
        return in;
    }
    @ParameterizedTest @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 99999999, 1000000000, Integer.MAX_VALUE})
    void invalidIdsRejected(int id) { assertThrows(IllegalArgumentException.class, () -> new ProfileId(id)); }
    @Test void idsAreStableValuesAndOrderNumerically() {
        assertEquals(new ProfileId(100000000), new ProfileId(100000000));
        assertEquals(new ProfileId(999999999).hashCode(), new ProfileId(999999999).hashCode());
        assertTrue(new ProfileId(100000009).compareTo(new ProfileId(100000010)) < 0);
    }
    @Test void draftDefaultsAndMinimumPublication() {
        ProfileFields draft = ProfileFields.from(new ProfileFields.Input(), TODAY);
        assertEquals(Set.of("displayName", "dateOfBirth", "audience"), draft.publicationErrors(TODAY).keySet());
        assertEquals(AudienceMode.UNSET, draft.getPreferences().getAudience());
        assertTrue(draft.getInterestTags().isEmpty()); assertTrue(draft.getPhotoIds().isEmpty()); assertFalse(draft.isShowGender());
        assertTrue(ProfileFields.from(minimum(), TODAY).publicationErrors(TODAY).isEmpty());
    }
    @Test void birthdaysAndLeapDayUseSpecifiedAnniversary() {
        ProfileFields.Input in = minimum(); in.dateOfBirth = TODAY.minusYears(18);
        assertEquals(18, ProfileFields.from(in, TODAY).ageOn(TODAY).orElseThrow());
        assertTrue(ProfileFields.from(in, TODAY).publicationErrors(TODAY).isEmpty());
        in.dateOfBirth = TODAY.minusYears(18).plusDays(1);
        assertEquals(FieldError.OUT_OF_RANGE, ProfileFields.from(in, TODAY).publicationErrors(TODAY).get("dateOfBirth"));
        in.dateOfBirth = LocalDate.of(2008, 2, 29);
        ProfileFields f = ProfileFields.from(in, TODAY);
        assertEquals(16, f.ageOn(LocalDate.of(2025, 2, 27)).orElseThrow());
        assertEquals(17, f.ageOn(LocalDate.of(2025, 2, 28)).orElseThrow());
        assertEquals(18, f.ageOn(LocalDate.of(2026, 2, 28)).orElseThrow());
    }
    @Test void normalizationPreservesUnicodeAndClearsWhitespace() {
        ProfileFields.Input in = minimum(); in.displayName = "  " + "😀".repeat(40) + "  "; in.bio = "  Hello\r\nworld\ragain  ";
        ProfileFields f = ProfileFields.from(in, TODAY);
        assertEquals(40, f.getDisplayName().orElseThrow().codePointCount(0, f.getDisplayName().orElseThrow().length()));
        assertEquals("Hello\nworld\nagain", f.getBio().orElseThrow());
        in.displayName = "  "; in.bio = "\n ";
        f = ProfileFields.from(in, TODAY); assertTrue(f.getBio().isEmpty()); assertTrue(f.getDisplayName().isEmpty());
    }
    @ParameterizedTest @ValueSource(strings = {"a\nb", "a\tb", "a\rb", "a\u2028b", "a\u2029b", "\u0000name"})
    void displayNameRejectsControlsAndSeparators(String name) {
        ProfileFields.Input in = minimum(); in.displayName = name;
        assertEquals(FieldError.INVALID_VALUE, assertThrows(ProfileValidationException.class, () -> ProfileFields.from(in, TODAY)).getErrors().get("displayName"));
    }
    @Test void independentErrorsAndNestedTupleErrorsAccumulate() {
        ProfileFields.Input in = minimum(); in.displayName = "x".repeat(41); in.bio = "a\tb"; in.dateOfBirth = TODAY.plusDays(1);
        in.gender = "UNKNOWN"; in.locationArea = "PARIS"; in.smoking = "OFTEN"; in.drinking = "?"; in.relationshipIntent = "?";
        in.minYears = 17; in.ageMode = "WRONG"; in.distanceMode = "HARD";
        in.photoIds = Arrays.asList(UUID.randomUUID(), null);
        in.interestTags = List.of("ART", "ART"); in.preferredInterestTags = List.of("WRONG");
        Map<String, FieldError> errors = assertThrows(ProfileValidationException.class, () -> ProfileFields.from(in, TODAY)).getErrors();
        assertEquals(Set.of("displayName", "bio", "dateOfBirth", "gender", "locationArea", "relationshipIntent", "smoking", "drinking",
                "photoIds", "interestTags", "preferredInterestTags", "agePreference.minYears", "agePreference.maxYears",
                "agePreference.mode", "distancePreference.maxKm"), errors.keySet());
        assertEquals(FieldError.REQUIRED, errors.get("agePreference.maxYears"));
        assertEquals(FieldError.REQUIRED, errors.get("distancePreference.maxKm"));
        assertEquals(FieldError.OUT_OF_RANGE, errors.get("displayName"));
        assertEquals(FieldError.INVALID_VALUE, errors.get("photoIds"));
        assertThrows(UnsupportedOperationException.class, () -> errors.clear());
    }
    @Test void limitsAndClosedCatalogs() {
        ProfileFields.Input in = minimum(); in.bio = "😀".repeat(500);
        in.interestTags = List.of("ART", "BOARD_GAMES", "BOOKS", "COOKING", "FILM", "FITNESS", "GAMING", "HIKING", "MUSIC", "SPORTS");
        in.preferredInterestTags = in.interestTags;
        in.photoIds = new ArrayList<>(); for (int i=0; i<6; i++) in.photoIds.add(UUID.randomUUID());
        ProfileFields.from(in, TODAY);
        in.bio += "x"; in.photoIds.add(UUID.randomUUID());
        in.interestTags = new ArrayList<>(in.interestTags); in.interestTags.add("TRAVEL"); in.preferredInterestTags = in.interestTags;
        Map<String, FieldError> errors = assertThrows(ProfileValidationException.class, () -> ProfileFields.from(in, TODAY)).getErrors();
        assertEquals(Set.of("bio", "photoIds", "interestTags", "preferredInterestTags"), errors.keySet());
        assertTrue(errors.values().stream().allMatch(v -> v == FieldError.OUT_OF_RANGE));
        for (Gender gender : Gender.values()) { ProfileFields.Input sample = minimum(); sample.gender = gender.name(); assertEquals(gender, ProfileFields.from(sample, TODAY).getGender().orElseThrow()); }
    }
    @Test void collectionsAreCopiedAndUnknownIsDifferentFromEmpty() {
        ProfileFields.Input in = minimum(); List<String> tags = new ArrayList<>(List.of("ART"));
        List<UUID> photos = new ArrayList<>(List.of(UUID.randomUUID())); in.interestTags = tags; in.photoIds = photos;
        ProfileFields f = ProfileFields.from(in, TODAY); tags.clear(); photos.clear();
        assertEquals(Set.of(InterestTag.ART), f.getInterestTags().orElseThrow()); assertEquals(1, f.getPhotoIds().size());
        assertThrows(UnsupportedOperationException.class, () -> f.getPhotoIds().clear());
        assertThrows(UnsupportedOperationException.class, () -> f.getInterestTags().orElseThrow().clear());
        in.interestTags = null; ProfileFields unknown = ProfileFields.from(in, TODAY);
        in.interestTags = List.of(); ProfileFields empty = ProfileFields.from(in, TODAY);
        assertNotEquals(unknown, empty); assertEquals(Optional.of(Set.of()), empty.getInterestTags());
        assertEquals(empty, ProfileFields.from(in, TODAY)); assertEquals(empty.hashCode(), ProfileFields.from(in, TODAY).hashCode());
    }
    @Test void audienceAndPreferenceInvariants() {
        assertThrows(IllegalArgumentException.class, () -> new AgePreference(17, 30, PreferenceMode.SOFT));
        assertThrows(IllegalArgumentException.class, () -> new AgePreference(40, 30, PreferenceMode.SOFT));
        assertThrows(IllegalArgumentException.class, () -> new AgePreference(18, 121, PreferenceMode.HARD));
        assertThrows(NullPointerException.class, () -> new AgePreference(18, 120, null));
        new AgePreference(18, 120, PreferenceMode.HARD);
        assertThrows(IllegalArgumentException.class, () -> new DistancePreference(0, PreferenceMode.HARD));
        assertThrows(IllegalArgumentException.class, () -> new DistancePreference(501, PreferenceMode.SOFT));
        new DistancePreference(1, PreferenceMode.SOFT); new DistancePreference(500, PreferenceMode.HARD);
        assertThrows(IllegalArgumentException.class, () -> new PartnerPreferences(AudienceMode.SELECTED, Set.of(), Set.of(), null, null));
        assertThrows(IllegalArgumentException.class, () -> new PartnerPreferences(AudienceMode.ANY, Set.of(Gender.WOMAN), Set.of(), null, null));
        ProfileFields.Input in = minimum(); in.audience = "SELECTED";
        assertEquals(FieldError.REQUIRED, assertThrows(ProfileValidationException.class, () -> ProfileFields.from(in, TODAY)).getErrors().get("audience.acceptedGenders"));
    }
    @Test void matchingAndPublicProjectionsShareIdentityRevisionAndDate() {
        ProfileFields.Input in = minimum(); in.gender = "WOMAN"; in.showGender = false;
        in.smoking = "NEVER"; in.drinking = "SOMETIMES"; in.relationshipIntent = "LONG_TERM"; in.locationArea = "MARKHAM";
        DiscoveryProfile stored = new DiscoveryProfile(new ProfileId(100000001), 7, DiscoveryStatus.ACTIVE, ProfileFields.from(in, TODAY));
        MatchingProfile matching = stored.matchingOn(TODAY); PublicProfileSummary publicProfile = stored.publicOn(TODAY);
        assertEquals(matching.getProfileId(), publicProfile.getProfileId()); assertEquals(7, publicProfile.getProfileRevision());
        assertEquals(26, publicProfile.getAgeYears()); assertEquals(Optional.of(Gender.WOMAN), matching.getGender());
        assertEquals(Optional.of(Frequency.NEVER), stored.getFields().getSmoking());
        assertEquals(Optional.of(Frequency.SOMETIMES), stored.getFields().getDrinking());
        assertEquals(Optional.of(RelationshipIntent.LONG_TERM), stored.getFields().getRelationshipIntent());
        assertThrows(IllegalArgumentException.class, () -> new DiscoveryProfile(new ProfileId(100000001), 0, DiscoveryStatus.DRAFT, stored.getFields()));
    }
}
