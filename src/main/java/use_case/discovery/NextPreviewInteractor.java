package use_case.discovery;

import entity.matchmaking.*;
import java.time.*;
import java.util.*;
import static use_case.discovery.NextPreviewOutputData.Status.*;

public final class NextPreviewInteractor implements NextPreviewInputBoundary {
    private final ViewerSessionDataAccessInterface sessions;
    private final DiscoveryDataAccessInterface discovery;
    private final NextPreviewOutputBoundary output;
    private final EligibilityPolicy eligibility;
    private final ViewerScorer scorer;
    private final Clock clock;
    public NextPreviewInteractor(ViewerSessionDataAccessInterface sessions, DiscoveryDataAccessInterface discovery,
                                 NextPreviewOutputBoundary output, EligibilityPolicy eligibility, ViewerScorer scorer, Clock clock) {
        this.sessions = Objects.requireNonNull(sessions); this.discovery = Objects.requireNonNull(discovery);
        this.output = Objects.requireNonNull(output); this.eligibility = Objects.requireNonNull(eligibility);
        this.scorer = Objects.requireNonNull(scorer); this.clock = Objects.requireNonNull(clock).withZone(ZoneId.of("America/Toronto"));
    }
    @Override public void execute(NextPreviewInputData input) {
        Objects.requireNonNull(input);
        LocalDate effectiveDate = LocalDate.now(clock);
        NextPreviewOutputData result;
        try { result = select(input, effectiveDate); }
        catch (DataAccessException e) { result = outcome(input, RETRYABLE_FAILURE); }
        // Deliberately outside the catch: a Presenter failure must never trigger a second presentation.
        output.present(result);
    }
    private NextPreviewOutputData select(NextPreviewInputData input, LocalDate date) {
        SessionReadResult session = sessions.resolve(input.getSessionToken());
        if (session.getStatus() == SessionReadResult.Status.SESSION_EXPIRED) return outcome(input, SESSION_EXPIRED);
        if (session.getStatus() == SessionReadResult.Status.UNAVAILABLE) return outcome(input, RETRYABLE_FAILURE);
        ResolvedViewer resolved = session.getViewer().orElseThrow();
        DiscoveryReadResult read = discovery.load(resolved, date);
        if (read.getStatus() == DiscoveryReadResult.Status.VIEWER_UNAVAILABLE) return outcome(input, VIEWER_UNAVAILABLE);
        if (read.getStatus() == DiscoveryReadResult.Status.UNAVAILABLE) return outcome(input, RETRYABLE_FAILURE);
        DiscoverySnapshot snapshot = read.getSnapshot().orElseThrow();
        if (!date.equals(snapshot.getEffectiveDate()) || !resolved.getViewerId().equals(snapshot.getViewer().getProfileId()))
            return outcome(input, RETRYABLE_FAILURE);
        MatchingProfile viewer = snapshot.getViewer();
        if (!eligibility.available(viewer)) return outcome(input, VIEWER_UNAVAILABLE);
        CandidateSnapshot best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        OptionalInt bestDistance = OptionalInt.empty();
        for (CandidateSnapshot candidate : snapshot.getCandidates()) {
            MatchingProfile profile = candidate.getProfile();
            OptionalInt distance = eligibility.distance(viewer, profile);
            if (!eligibility.eligible(viewer, profile, candidate.getPairState(), distance)) continue;
            double score = scorer.score(viewer, profile, distance);
            if (best == null || Double.compare(score, bestScore) > 0 || (Double.compare(score, bestScore) == 0
                    && profile.getProfileId().compareTo(best.getProfile().getProfileId()) < 0)) {
                best = candidate; bestScore = score; bestDistance = distance;
            }
        }
        if (best == null) return outcome(input, EMPTY);
        return new NextPreviewOutputData(input.getRequestId(), SELECTED,
                Optional.of(new SelectedCandidateData(best.getPublicProfile(), snapshot.getFeedRevision(), date, bestDistance)));
    }
    private NextPreviewOutputData outcome(NextPreviewInputData input, NextPreviewOutputData.Status status) {
        return new NextPreviewOutputData(input.getRequestId(), status, Optional.empty());
    }
}
