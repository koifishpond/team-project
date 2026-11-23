package entity.matchmaking;

/** Stable account identity; reconstructing it never allocates another ID. */
public final class ProfileId implements Comparable<ProfileId> {
    private final int value;
    public ProfileId(int value) {
        if (value < 100000000 || value > 999999999) throw new IllegalArgumentException("Nine-digit ID required");
        this.value = value;
    }
    public int getValue() { return value; }
    @Override public int compareTo(ProfileId other) { return Integer.compare(value, other.value); }
    @Override public boolean equals(Object o) { return o instanceof ProfileId && value == ((ProfileId) o).value; }
    @Override public int hashCode() { return Integer.hashCode(value); }
    @Override public String toString() { return Integer.toString(value); }
}
