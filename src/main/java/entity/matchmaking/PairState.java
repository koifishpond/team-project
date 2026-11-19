package entity.matchmaking;

/** Incoming likes are deliberately absent: they do not exclude a candidate. */
public final class PairState {
    private final boolean viewerBlockedCandidate, candidateBlockedViewer, viewerLikedCandidate, matched,
            viewerDislikedCandidate, viewerReportedCandidate, suppressedInCurrentSession;
    public PairState(boolean viewerBlockedCandidate, boolean candidateBlockedViewer, boolean viewerLikedCandidate,
                     boolean matched, boolean viewerDislikedCandidate, boolean viewerReportedCandidate, boolean suppressedInCurrentSession) {
        this.viewerBlockedCandidate = viewerBlockedCandidate; this.candidateBlockedViewer = candidateBlockedViewer;
        this.viewerLikedCandidate = viewerLikedCandidate; this.matched = matched;
        this.viewerDislikedCandidate = viewerDislikedCandidate; this.viewerReportedCandidate = viewerReportedCandidate;
        this.suppressedInCurrentSession = suppressedInCurrentSession;
    }
    public static PairState clear() { return new PairState(false, false, false, false, false, false, false); }
    public boolean excludes() {
        return viewerBlockedCandidate || candidateBlockedViewer || viewerLikedCandidate || matched
                || viewerDislikedCandidate || viewerReportedCandidate || suppressedInCurrentSession;
    }
}
