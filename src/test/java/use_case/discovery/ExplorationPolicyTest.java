package use_case.discovery;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

final class ExplorationPolicyTest {
    private final ExplorationPolicy policy = new ExplorationPolicy(new Random(17), 1, 0.20, 0.05, 0.05, 0.10, 10);

    @Test void eventCountAndPartialCohortsDoNotCountAsMeasuredImprovement() {
        List<MatureImpression> sample = new ArrayList<>();
        for (int i = 0; i < 8; i++) sample.addAll(cohort(3, 1, 1));
        assertEquals(0.20, policy.range(sample), 1e-12);
        sample.addAll(cohort(7, 2, 0).subList(0, 9));
        assertEquals(0.20, policy.range(sample), 1e-12);
    }

    @Test void improvementNarrowsToTheFloorAndDeteriorationResetsTheBand() {
        List<MatureImpression> sample = new ArrayList<>(cohort(2, 1, 1));
        sample.addAll(cohort(4, 1, 1));
        assertEquals(0.15, policy.range(sample), 1e-12);
        sample.addAll(cohort(6, 2, 1));
        sample.addAll(cohort(8, 2, 1));
        sample.addAll(cohort(9, 3, 0));
        assertEquals(0.05, policy.range(sample), 1e-12);
        sample.addAll(cohort(10, 2, 0)); // match deterioration overrides the gain in opens
        assertEquals(0.20, policy.range(sample), 1e-12);
        sample.addAll(cohort(10, 3, 1)); // a higher dislike rate prevents narrowing
        assertEquals(0.20, policy.range(sample), 1e-12);
    }

    @Test void frequencyAndGapAreIndependentAndSeedRepeatsAnAlternativeSequence() {
        List<Double> scores = List.of(0.8, 0.8, 0.7, 0.6, 0.59);
        ExplorationPolicy repeat = new ExplorationPolicy(new Random(17), 1, 0.20, 0.05, 0.05, 0.10, 10);
        Set<Integer> observed = new HashSet<>();
        Random ids = new Random(3);
        for (int i = 0; i < 20; i++) {
            UUID request = new UUID(ids.nextLong(), ids.nextLong());
            int index = policy.chooseIndex(scores, List.of(), request);
            assertEquals(index, policy.chooseIndex(scores, List.of(), request));
            assertEquals(index, repeat.chooseIndex(scores, List.of(), request));
            observed.add(index);
        }
        assertEquals(Set.of(2, 3), observed);
        UUID request = new UUID(0, 0);
        assertEquals(0, ExplorationPolicy.disabled().chooseIndex(scores, List.of(), request));
        assertEquals(0, new ExplorationPolicy(new Random(17), 1, 0.05, 0.05, 0, 0.10, 10).chooseIndex(scores, List.of(), request));
        assertThrows(IllegalArgumentException.class, () -> new ExplorationPolicy(new Random(), Double.NaN, 0.2, 0.05, 0.04, 0.10, 10));
    }

    private static List<MatureImpression> cohort(int opens, int matches, int dislikes) {
        List<MatureImpression> sample = new ArrayList<>();
        for (int i = 0; i < 10; i++) sample.add(new MatureImpression(i < opens, i < matches, i >= 10 - dislikes));
        return sample;
    }
}
