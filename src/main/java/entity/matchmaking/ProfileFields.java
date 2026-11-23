package entity.matchmaking;

import java.time.LocalDate;
import java.util.*;

/** Complete normalized editable v1 data. Birth date and partner settings are private. */
public final class ProfileFields {
    /** Full replacement input. Null scalar fields clear a value; null collections use schema defaults. */
    public static final class Input {
        public String displayName, bio, gender, locationArea, relationshipIntent, smoking, drinking;
        public LocalDate dateOfBirth;
        public boolean showGender;
        public String audience = "UNSET";
        public List<String> acceptedGenders, interestTags, preferredInterestTags;
        public List<UUID> photoIds;
        public Integer minYears, maxYears, maxKm;
        public String ageMode, distanceMode;
    }

    private final String displayName, bio;
    private final LocalDate dateOfBirth;
    private final Gender gender;
    private final boolean showGender;
    private final List<UUID> photoIds;
    private final Set<InterestTag> interestTags; // null means unanswered, empty means explicitly none
    private final LocationArea locationArea;
    private final RelationshipIntent relationshipIntent;
    private final Frequency smoking, drinking;
    private final PartnerPreferences preferences;

    private ProfileFields(Input in, LocalDate today, Map<String, FieldError> errors) {
        displayName = normalizeText(in.displayName, 40, false, "displayName", errors);
        bio = normalizeText(in.bio, 500, true, "bio", errors);
        dateOfBirth = in.dateOfBirth;
        if (dateOfBirth != null && dateOfBirth.isAfter(today)) errors.put("dateOfBirth", FieldError.OUT_OF_RANGE);
        gender = code(Gender.class, in.gender, "gender", errors);
        showGender = in.showGender;
        locationArea = code(LocationArea.class, in.locationArea, "locationArea", errors);
        relationshipIntent = code(RelationshipIntent.class, in.relationshipIntent, "relationshipIntent", errors);
        smoking = code(Frequency.class, in.smoking, "smoking", errors);
        drinking = code(Frequency.class, in.drinking, "drinking", errors);
        List<UUID> photos = in.photoIds == null ? Collections.emptyList() : new ArrayList<>(in.photoIds);
        if (photos.contains(null) || new HashSet<>(photos).size() != photos.size()) errors.put("photoIds", FieldError.INVALID_VALUE);
        if (photos.size() > 6) errors.put("photoIds", FieldError.OUT_OF_RANGE);
        photoIds = Collections.unmodifiableList(photos);
        interestTags = in.interestTags == null ? null : codes(InterestTag.class, in.interestTags, "interestTags", 10, errors);
        Set<InterestTag> preferred = codes(InterestTag.class, in.preferredInterestTags, "preferredInterestTags", 10, errors);
        Set<Gender> accepted = codes(Gender.class, in.acceptedGenders, "audience.acceptedGenders", 4, errors);
        AudienceMode audience = code(AudienceMode.class, in.audience == null ? "UNSET" : in.audience, "audience.mode", errors);
        if (audience == AudienceMode.SELECTED && accepted.isEmpty()) errors.put("audience.acceptedGenders", FieldError.REQUIRED);
        if (audience != null && audience != AudienceMode.SELECTED && !accepted.isEmpty()) errors.put("audience.acceptedGenders", FieldError.INVALID_VALUE);

        AgePreference age = null;
        if (in.minYears != null || in.maxYears != null || in.ageMode != null) {
            if (in.minYears == null) errors.put("agePreference.minYears", FieldError.REQUIRED);
            if (in.maxYears == null) errors.put("agePreference.maxYears", FieldError.REQUIRED);
            PreferenceMode mode = code(PreferenceMode.class, in.ageMode, "agePreference.mode", errors);
            if (mode == null && in.ageMode == null) errors.put("agePreference.mode", FieldError.REQUIRED);
            if (in.minYears != null && (in.minYears < 18 || in.minYears > 120)) errors.put("agePreference.minYears", FieldError.OUT_OF_RANGE);
            if (in.maxYears != null && (in.maxYears < 18 || in.maxYears > 120 || (in.minYears != null && in.minYears > in.maxYears)))
                errors.put("agePreference.maxYears", FieldError.OUT_OF_RANGE);
            if (errors.keySet().stream().noneMatch(k -> k.startsWith("agePreference."))) age = new AgePreference(in.minYears, in.maxYears, mode);
        }
        DistancePreference distance = null;
        if (in.maxKm != null || in.distanceMode != null) {
            if (in.maxKm == null) errors.put("distancePreference.maxKm", FieldError.REQUIRED);
            else if (in.maxKm < 1 || in.maxKm > 500) errors.put("distancePreference.maxKm", FieldError.OUT_OF_RANGE);
            PreferenceMode mode = code(PreferenceMode.class, in.distanceMode, "distancePreference.mode", errors);
            if (mode == null && in.distanceMode == null) errors.put("distancePreference.mode", FieldError.REQUIRED);
            if (errors.keySet().stream().noneMatch(k -> k.startsWith("distancePreference."))) distance = new DistancePreference(in.maxKm, mode);
        }
        // No partly validated value is returned. The fallback only allows independent errors to accumulate.
        boolean validAudience = audience != null && ((audience == AudienceMode.SELECTED) != accepted.isEmpty());
        preferences = new PartnerPreferences(validAudience ? audience : AudienceMode.UNSET,
                validAudience ? accepted : Set.of(), preferred.size() <= 10 ? preferred : Set.of(), age, distance);
    }

    public static ProfileFields from(Input input, LocalDate today) {
        return from(input, today, false);
    }

    public static ProfileFields from(Input input, LocalDate today, boolean requirePublished) {
        Map<String, FieldError> errors = new LinkedHashMap<>();
        ProfileFields fields = new ProfileFields(Objects.requireNonNull(input), Objects.requireNonNull(today), errors);
        if (requirePublished) fields.publicationErrors(today).forEach(errors::putIfAbsent);
        if (!errors.isEmpty()) throw new ProfileValidationException(errors);
        return fields;
    }

    static String normalizeText(String value, int max, boolean multiline, String key, Map<String, FieldError> errors) {
        if (value == null) return null;
        String normalized = (multiline ? value.replace("\r\n", "\n").replace('\r', '\n') : value).strip();
        // Validate before stripping so control characters cannot disappear at the ends.
        if (value.codePoints().anyMatch(c -> (Character.isISOControl(c) && !(multiline && (c == '\n' || c == '\r')))
                || (!multiline && (c == 0x2028 || c == 0x2029)))) errors.put(key, FieldError.INVALID_VALUE);
        if (normalized.codePointCount(0, normalized.length()) > max) errors.put(key, FieldError.OUT_OF_RANGE);
        return normalized.isEmpty() ? null : normalized;
    }

    private static <E extends Enum<E>> E code(Class<E> type, String value, String key, Map<String, FieldError> errors) {
        if (value == null) return null;
        try { return Enum.valueOf(type, value); }
        catch (IllegalArgumentException e) { errors.put(key, FieldError.INVALID_VALUE); return null; }
    }

    private static <E extends Enum<E>> Set<E> codes(Class<E> type, List<String> values, String key, int max, Map<String, FieldError> errors) {
        Set<E> result = EnumSet.noneOf(type);
        if (values != null) {
            for (String value : new ArrayList<>(values)) {
                E parsed = code(type, value, key, errors);
                if (parsed == null || !result.add(parsed)) errors.put(key, FieldError.INVALID_VALUE);
            }
            if (values.size() > max) errors.put(key, FieldError.OUT_OF_RANGE);
        }
        return Collections.unmodifiableSet(result);
    }

    public Map<String, FieldError> publicationErrors(LocalDate date) {
        Map<String, FieldError> errors = new LinkedHashMap<>();
        if (displayName == null) errors.put("displayName", FieldError.REQUIRED);
        if (dateOfBirth == null) errors.put("dateOfBirth", FieldError.REQUIRED);
        else if (date.isBefore(dateOfBirth.plusYears(18))) errors.put("dateOfBirth", FieldError.OUT_OF_RANGE);
        if (preferences.getAudience() == AudienceMode.UNSET) errors.put("audience", FieldError.REQUIRED);
        return Collections.unmodifiableMap(errors);
    }

    public OptionalInt ageOn(LocalDate date) {
        if (dateOfBirth == null) return OptionalInt.empty();
        int years = date.getYear() - dateOfBirth.getYear();
        return OptionalInt.of(date.isBefore(dateOfBirth.plusYears(years)) ? years - 1 : years);
    }
    public Optional<String> getDisplayName() { return Optional.ofNullable(displayName); }
    public Optional<String> getBio() { return Optional.ofNullable(bio); }
    public Optional<LocalDate> getDateOfBirth() { return Optional.ofNullable(dateOfBirth); }
    public Optional<Gender> getGender() { return Optional.ofNullable(gender); }
    public boolean isShowGender() { return showGender; }
    public List<UUID> getPhotoIds() { return photoIds; }
    public Optional<Set<InterestTag>> getInterestTags() { return Optional.ofNullable(interestTags); }
    public Optional<LocationArea> getLocationArea() { return Optional.ofNullable(locationArea); }
    public Optional<RelationshipIntent> getRelationshipIntent() { return Optional.ofNullable(relationshipIntent); }
    public Optional<Frequency> getSmoking() { return Optional.ofNullable(smoking); }
    public Optional<Frequency> getDrinking() { return Optional.ofNullable(drinking); }
    public PartnerPreferences getPreferences() { return preferences; }

    @Override public boolean equals(Object o) {
        if (!(o instanceof ProfileFields)) return false;
        ProfileFields f = (ProfileFields) o;
        return showGender == f.showGender && Objects.equals(displayName, f.displayName) && Objects.equals(bio, f.bio)
            && Objects.equals(dateOfBirth, f.dateOfBirth) && gender == f.gender && photoIds.equals(f.photoIds)
            && Objects.equals(interestTags, f.interestTags) && locationArea == f.locationArea && relationshipIntent == f.relationshipIntent
            && smoking == f.smoking && drinking == f.drinking && preferences.equals(f.preferences);
    }
    @Override public int hashCode() {
        return Objects.hash(displayName, bio, dateOfBirth, gender, showGender, photoIds, interestTags, locationArea,
                relationshipIntent, smoking, drinking, preferences);
    }
}
