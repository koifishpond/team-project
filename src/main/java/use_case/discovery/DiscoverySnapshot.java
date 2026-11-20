package use_case.discovery;

import entity.matchmaking.*;
import java.time.LocalDate;
import java.util.*;

public final class DiscoverySnapshot {
    private final long feedRevision;
    private final int schemaVersion;
    private final LocalDate effectiveDate;
    private final MatchingProfile viewer;
    private final ViewerPreferenceVector viewerPreferences;
    private final List<CandidateSnapshot> candidates;
    private final List<MatureImpression> explorationEvidence;
    public DiscoverySnapshot(long revision, int schemaVersion, LocalDate date, MatchingProfile viewer, List<CandidateSnapshot> candidates) {
        this(revision, schemaVersion, date, viewer, candidates, DiscoveryFeatures.preferences(viewer.getPreferences()));
    }

    public DiscoverySnapshot(long revision, int schemaVersion, LocalDate date, MatchingProfile viewer,
                             List<CandidateSnapshot> candidates, ViewerPreferenceVector preferences) {
        this(revision, schemaVersion, date, viewer, candidates, preferences, List.of());
    }

    public DiscoverySnapshot(long revision, int schemaVersion, LocalDate date, MatchingProfile viewer,
                             List<CandidateSnapshot> candidates, ViewerPreferenceVector preferences,
                             List<MatureImpression> explorationEvidence) {
        if (revision < 1 || schemaVersion != 1) throw new IllegalArgumentException("Snapshot version");
        feedRevision = revision; this.schemaVersion = schemaVersion;
        effectiveDate = Objects.requireNonNull(date); this.viewer = Objects.requireNonNull(viewer);
        DiscoveryFeatures.requireCurrent(preferences.version(), preferences.dimension());
        viewerPreferences = preferences;
        this.candidates = List.copyOf(candidates);
        this.explorationEvidence = List.copyOf(explorationEvidence);
        Set<ProfileId> ids = new HashSet<>();
        for (CandidateSnapshot candidate : this.candidates) {
            if (!ids.add(candidate.getProfile().getProfileId())) throw new IllegalArgumentException("Duplicate candidate");
        }
    }
    public long getFeedRevision() { return feedRevision; }
    public int getSchemaVersion() { return schemaVersion; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public MatchingProfile getViewer() { return viewer; }
    public ViewerPreferenceVector getViewerPreferences() { return viewerPreferences; }
    public List<CandidateSnapshot> getCandidates() { return candidates; }
    public List<MatureImpression> getExplorationEvidence() { return explorationEvidence; }
}
