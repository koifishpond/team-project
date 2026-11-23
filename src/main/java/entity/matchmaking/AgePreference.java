package entity.matchmaking;

import java.util.Objects;
public final class AgePreference {
    private final int minYears, maxYears;
    private final PreferenceMode mode;
    public AgePreference(int minYears, int maxYears, PreferenceMode mode) {
        if (minYears < 18 || maxYears > 120 || minYears > maxYears) throw new IllegalArgumentException("Age range");
        this.minYears = minYears; this.maxYears = maxYears; this.mode = Objects.requireNonNull(mode);
    }
    public int getMinYears() { return minYears; }
    public int getMaxYears() { return maxYears; }
    public PreferenceMode getMode() { return mode; }
    @Override public boolean equals(Object o) {
        if (!(o instanceof AgePreference)) return false;
        AgePreference a = (AgePreference) o;
        return minYears == a.minYears && maxYears == a.maxYears && mode == a.mode;
    }
    @Override public int hashCode() { return Objects.hash(minYears, maxYears, mode); }
}
