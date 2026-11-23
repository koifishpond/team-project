package entity.matchmaking;

import java.util.BitSet;

/** Encoded candidate facts. Unknown coordinates are explicitly masked. */
public final class CandidateAttributeVector {
    private final int version;
    private final double[] values;
    private final BitSet known;

    public CandidateAttributeVector(int version, double[] values, BitSet known) {
        this.version = version;
        this.values = values.clone();
        this.known = (BitSet) known.clone();
        if (version < 1 || values.length == 0 || this.known.length() > values.length) {
            throw new IllegalArgumentException("Invalid candidate vector shape");
        }
        for (int i = 0; i < this.values.length; i++) {
            double value = this.values[i];
            if (!Double.isFinite(value) || Math.abs(value) > 1 || (!this.known.get(i) && value != 0)) {
                throw new IllegalArgumentException("Invalid candidate coordinate");
            }
        }
    }

    public int version() { return version; }
    public int dimension() { return values.length; }
    public boolean isKnown(int index) { return known.get(index); }
    public double valueAt(int index) { return values[index]; }
}
