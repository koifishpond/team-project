package entity.matchmaking;

import java.util.Objects;
public final class DistancePreference {
    private final int maxKm;
    private final PreferenceMode mode;
    public DistancePreference(int maxKm, PreferenceMode mode) {
        if (maxKm < 1 || maxKm > 500) throw new IllegalArgumentException("Distance range");
        this.maxKm = maxKm; this.mode = Objects.requireNonNull(mode);
    }
    public int getMaxKm() { return maxKm; }
    public PreferenceMode getMode() { return mode; }
    @Override public boolean equals(Object o) {
        return o instanceof DistancePreference && maxKm == ((DistancePreference) o).maxKm && mode == ((DistancePreference) o).mode;
    }
    @Override public int hashCode() { return Objects.hash(maxKm, mode); }
}
