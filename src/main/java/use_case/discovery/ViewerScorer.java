package use_case.discovery;

import entity.matchmaking.CandidateAttributeVector;
import entity.matchmaking.ViewerPreferenceVector;

/** Directional masked dot product normalized by the viewer's maximum feature budget. */
public final class ViewerScorer {
    public double score(ViewerPreferenceVector viewer, CandidateAttributeVector candidate) {
        DiscoveryFeatures.requireCurrent(viewer.version(), viewer.dimension());
        DiscoveryFeatures.requireCurrent(candidate.version(), candidate.dimension());
        double dot = 0;
        double[] budgets = new double[DiscoveryFeatures.Group.values().length];
        for (int i = 0; i < viewer.dimension(); i++) {
            double affinity = viewer.affinityAt(i);
            if (candidate.isKnown(i)) dot += affinity * candidate.valueAt(i);
            DiscoveryFeatures.Group group = DiscoveryFeatures.groupAt(i);
            int index = group.ordinal();
            // Interests can co-occur. Other groups activate at most one coordinate.
            if (group == DiscoveryFeatures.Group.INTEREST) budgets[index] += Math.abs(affinity);
            else budgets[index] = Math.max(budgets[index], Math.abs(affinity));
        }
        double budget = 0;
        for (double value : budgets) budget += value;
        double similarity = dot / Math.max(1, budget);
        return 0.5 + 0.5 * Math.max(-1, Math.min(1, similarity));
    }
}
