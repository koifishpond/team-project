package use_case.discovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/** Separates how often to explore from how far below the best fit the selection may go. */
public final class ExplorationPolicy {
    private static final double EPSILON = 1e-12;
    private final long seed;
    private final double frequency, initialRange, minimumRange, step, meaningfulChange;
    private final int cohortSize;

    public ExplorationPolicy(Random random, double frequency, double initialRange, double minimumRange,
                             double step, double meaningfulChange, int cohortSize) {
        seed = Objects.requireNonNull(random).nextLong();
        if (!unit(frequency) || !unit(initialRange) || !unit(minimumRange) || !unit(step) || !unit(meaningfulChange)
                || minimumRange > initialRange || meaningfulChange == 0 || cohortSize < 1) {
            throw new IllegalArgumentException("Invalid exploration settings");
        }
        this.frequency = frequency;
        this.initialRange = initialRange;
        this.minimumRange = minimumRange;
        this.step = step;
        this.meaningfulChange = meaningfulChange;
        this.cohortSize = cohortSize;
    }

    public static ExplorationPolicy standard(Random random) {
        return new ExplorationPolicy(random, 0.10, 0.20, 0.04, 0.04, 0.10, 20);
    }

    public static ExplorationPolicy disabled() {
        return new ExplorationPolicy(new Random(0), 0, 0, 0, 0, 0.10, 20);
    }

    private static boolean unit(double value) { return Double.isFinite(value) && value >= 0 && value <= 1; }

    /** Compare disjoint complete cohorts, replayed from the current preference epoch. */
    public double range(List<MatureImpression> sample) {
        double range = initialRange;
        Rates prior = null;
        for (int end = cohortSize; end <= sample.size(); end += cohortSize) {
            Rates current = new Rates(sample.subList(end - cohortSize, end));
            if (prior != null) {
                double openChange = current.opens - prior.opens;
                double matchChange = current.matches - prior.matches;
                double dislikeChange = current.dislikes - prior.dislikes;
                if (openChange <= -meaningfulChange + EPSILON || matchChange <= -meaningfulChange + EPSILON
                        || dislikeChange >= meaningfulChange - EPSILON) {
                    range = initialRange;
                } else if ((openChange >= meaningfulChange - EPSILON || matchChange >= meaningfulChange - EPSILON)
                        && openChange >= 0 && matchChange >= 0 && dislikeChange <= 0) {
                    range = Math.max(minimumRange, range - step);
                }
            }
            prior = current;
        }
        return range;
    }

    /** Input is the already-filtered pool in descending rank, including the numeric-ID tie break. */
    int chooseIndex(List<Double> scores, List<MatureImpression> sample, UUID requestId) {
        if (scores.size() < 2 || frequency == 0) return 0;
        double best = scores.get(0), allowed = range(sample);
        List<Integer> alternatives = new ArrayList<>();
        for (int i = 1; i < scores.size(); i++) {
            double gap = best - scores.get(i);
            if (gap > allowed + EPSILON) break;
            if (gap > 0) alternatives.add(i);
        }
        // Identical requests can be retried without consuming another exploration draw.
        long requestSeed = seed ^ requestId.getMostSignificantBits() ^ Long.rotateLeft(requestId.getLeastSignificantBits(), 29);
        Random random = new Random(requestSeed);
        if (alternatives.isEmpty() || random.nextDouble() >= frequency) return 0;
        return alternatives.get(random.nextInt(alternatives.size()));
    }

    private static final class Rates {
        private final double opens, matches, dislikes;

        private Rates(List<MatureImpression> cohort) {
            double count = cohort.size();
            opens = cohort.stream().filter(MatureImpression::opened).count() / count;
            matches = cohort.stream().filter(MatureImpression::matched).count() / count;
            dislikes = cohort.stream().filter(MatureImpression::disliked).count() / count;
        }
    }
}
