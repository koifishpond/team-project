package use_case.discovery;

import entity.matchmaking.*;
import java.util.*;

/** Version 1 feature schema. Keys are namespaced catalog codes, never user-entered text. */
public final class DiscoveryFeatures {
    public static final int VERSION = 1;
    private static final int MAX_AGE = 120;
    private static final int DISTANCE_STEP_KM = 5;
    private static final int MAX_DISTANCE_KM = 500;
    private static final String OLDER = "age:over120";
    private static final String FARTHER = "distance:over500";
    enum Group { INTEREST, AGE, DISTANCE, INTENT, SMOKING, DRINKING }
    private static final Map<String, Integer> INDEX;
    private static final List<Group> GROUPS;

    static {
        Map<String, Group> schema = new LinkedHashMap<>();
        for (InterestTag tag : InterestTag.values()) schema.put("interest:" + tag.name(), Group.INTEREST);
        for (int age = 18; age <= MAX_AGE; age++) schema.put("age:" + age, Group.AGE);
        schema.put(OLDER, Group.AGE);
        for (int km = 0; km <= MAX_DISTANCE_KM; km += DISTANCE_STEP_KM) schema.put("distance:" + km, Group.DISTANCE);
        schema.put(FARTHER, Group.DISTANCE);
        for (RelationshipIntent intent : RelationshipIntent.values()) schema.put("intent:" + intent.name(), Group.INTENT);
        for (Frequency value : Frequency.values()) schema.put("smoking:" + value.name(), Group.SMOKING);
        for (Frequency value : Frequency.values()) schema.put("drinking:" + value.name(), Group.DRINKING);
        Map<String, Integer> indexes = new HashMap<>();
        for (String key : schema.keySet()) indexes.put(key, indexes.size());
        INDEX = Map.copyOf(indexes);
        GROUPS = List.copyOf(schema.values());
    }

    private DiscoveryFeatures() { }

    public static int dimension() { return GROUPS.size(); }

    public static int indexOf(String feature) {
        Integer index = INDEX.get(feature);
        if (index == null) throw new IllegalArgumentException("Unknown discovery feature: " + feature);
        return index;
    }

    public static boolean recognizes(String feature) { return INDEX.containsKey(feature); }

    static Group groupAt(int index) { return GROUPS.get(index); }

    public static void requireCurrent(int version, int dimension) {
        if (version != VERSION || dimension != dimension()) throw new IllegalArgumentException("Unsupported discovery vector schema");
    }

    public static CandidateAttributeVector candidate(MatchingProfile profile, OptionalInt metres) {
        double[] values = new double[dimension()];
        BitSet known = new BitSet(dimension());
        profile.getInterestTags().ifPresent(tags -> {
            for (InterestTag tag : InterestTag.values()) {
                int index = indexOf("interest:" + tag.name());
                known.set(index);
                values[index] = tags.contains(tag) ? 1 : -1;
            }
        });
        if (profile.getAgeYears().isPresent() && profile.getAgeYears().getAsInt() >= 18) {
            int age = profile.getAgeYears().getAsInt();
            setCategory(values, known, Group.AGE, age > MAX_AGE ? OLDER : "age:" + age);
        }
        if (metres.isPresent()) {
            int distance = metres.getAsInt();
            if (distance < 0) throw new IllegalArgumentException("Negative distance");
            setCategory(values, known, Group.DISTANCE, distanceKey(distance));
        }
        profile.getRelationshipIntent().ifPresent(v -> setCategory(values, known, Group.INTENT, "intent:" + v.name()));
        profile.getSmoking().ifPresent(v -> setCategory(values, known, Group.SMOKING, "smoking:" + v.name()));
        profile.getDrinking().ifPresent(v -> setCategory(values, known, Group.DRINKING, "drinking:" + v.name()));
        return new CandidateAttributeVector(VERSION, values, known);
    }

    static String ageKey(int age) { return age > MAX_AGE ? OLDER : "age:" + age; }

    static String distanceKey(int distance) {
        long upperKm = DISTANCE_STEP_KM * ((distance + 4999L) / 5000);
        return upperKm > MAX_DISTANCE_KM ? FARTHER : "distance:" + upperKm;
    }

    public static Set<String> publicKeys(int age, OptionalInt distance, Collection<InterestTag> interests) {
        Set<String> keys = new HashSet<>();
        keys.add(ageKey(age));
        if (distance.isPresent()) keys.add(distanceKey(distance.getAsInt()));
        for (InterestTag tag : interests) keys.add("interest:" + tag.name());
        return Set.copyOf(keys);
    }

    public static Set<String> detailKeys(MatchingProfile profile, OptionalInt distance) {
        Set<String> keys = new HashSet<>(publicKeys(profile.getAgeYears().orElseThrow(), distance,
                profile.getInterestTags().orElse(Set.of())));
        profile.getRelationshipIntent().ifPresent(intent -> keys.add("intent:" + intent.name()));
        profile.getSmoking().ifPresent(value -> keys.add("smoking:" + value.name()));
        profile.getDrinking().ifPresent(value -> keys.add("drinking:" + value.name()));
        return Set.copyOf(keys);
    }

    private static void setCategory(double[] values, BitSet known, Group group, String activeKey) {
        for (int i = 0; i < dimension(); i++) {
            if (groupAt(i) == group) known.set(i);
        }
        values[indexOf(activeKey)] = 1;
    }

    public static ViewerPreferenceVector preferences(PartnerPreferences stated) {
        return preferences(stated, Collections.emptyMap());
    }

    public static ViewerPreferenceVector preferences(PartnerPreferences stated, Map<String, Double> learned) {
        double[] values = new double[dimension()];
        for (InterestTag tag : stated.getPreferredInterestTags()) values[indexOf("interest:" + tag.name())] = 0.5;
        stated.getAgePreference().ifPresent(age -> {
            for (int years = 18; years <= MAX_AGE; years++) {
                int outside = Math.max(0, Math.max(age.getMinYears() - years, years - age.getMaxYears()));
                values[indexOf("age:" + years)] = 0.5 * Math.max(0, 1 - outside / 10.0);
            }
            int outside = MAX_AGE + 1 - age.getMaxYears();
            values[indexOf(OLDER)] = 0.5 * Math.max(0, 1 - outside / 10.0);
        });
        stated.getDistancePreference().ifPresent(distance -> {
            for (int km = 0; km <= MAX_DISTANCE_KM; km += DISTANCE_STEP_KM) {
                double representative = km == 0 ? 0 : km - DISTANCE_STEP_KM / 2.0;
                values[indexOf("distance:" + km)] = 0.5 * Math.max(0, 1 - representative / distance.getMaxKm());
            }
        });
        learned.forEach((key, delta) -> {
            if (!Double.isFinite(delta)) throw new IllegalArgumentException("Nonfinite learned affinity");
            int index = indexOf(key);
            values[index] = clamp(values[index] + clamp(delta));
        });
        return new ViewerPreferenceVector(VERSION, values);
    }

    private static double clamp(double value) { return Math.max(-1, Math.min(1, value)); }
}
