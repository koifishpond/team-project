package entity.matchmaking;

/** Signed affinities in a versioned feature space, separate from editable partner settings. */
public final class ViewerPreferenceVector {
    private final int version;
    private final double[] affinities;

    public ViewerPreferenceVector(int version, double[] affinities) {
        this.version = version;
        this.affinities = affinities.clone();
        if (version < 1 || affinities.length == 0) throw new IllegalArgumentException("Invalid preference vector shape");
        for (double affinity : this.affinities) {
            if (!Double.isFinite(affinity) || Math.abs(affinity) > 1) {
                throw new IllegalArgumentException("Invalid preference coordinate");
            }
        }
    }

    public int version() { return version; }
    public int dimension() { return affinities.length; }
    public double affinityAt(int index) { return affinities[index]; }
}
