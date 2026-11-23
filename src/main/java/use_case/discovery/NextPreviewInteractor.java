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
    private final ExplorationPolicy exploration;
    public NextPreviewInteractor(ViewerSessionDataAccessInterface sessions, DiscoveryDataAccessInterface discovery,
                                 NextPreviewOutputBoundary output, EligibilityPolicy eligibility, ViewerScorer scorer, Clock clock) {
        this(sessions, discovery, output, eligibility, scorer, clock, ExplorationPolicy.standard(new Random()));
    }

    public NextPreviewInteractor(ViewerSessionDataAccessInterface sessions, DiscoveryDataAccessInterface discovery,
                                 NextPreviewOutputBoundary output, EligibilityPolicy eligibility, ViewerScorer scorer,
                                 Clock clock, ExplorationPolicy exploration) {
        this.sessions = Objects.requireNonNull(sessions); this.discovery = Objects.requireNonNull(discovery);
        this.output = Objects.requireNonNull(output); this.eligibility = Objects.requireNonNull(eligibility);
        this.scorer = Objects.requireNonNull(scorer); this.clock = Objects.requireNonNull(clock).withZone(ZoneId.of("America/Toronto"));
        this.exploration = Objects.requireNonNull(exploration);
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
        List<RankedCandidate> eligible = new ArrayList<>();
        for (CandidateSnapshot candidate : snapshot.getCandidates()) {
            MatchingProfile profile = candidate.getProfile();
            OptionalInt distance = eligibility.distance(viewer, profile);
            if (!eligibility.eligible(viewer, profile, candidate.getPairState(), distance)) continue;
            CandidateAttributeVector attributes = DiscoveryFeatures.candidate(profile, distance);
            double score = scorer.score(snapshot.getViewerPreferences(), attributes);
            eligible.add(new RankedCandidate(candidate, score, distance));
        }
        if (eligible.isEmpty()) return outcome(input, EMPTY);
        eligible.sort(Comparator.comparingDouble((RankedCandidate item) -> item.score).reversed()
                .thenComparing(item -> item.candidate.getProfile().getProfileId()));
        List<Double> scores = new ArrayList<>(eligible.size());
        for (RankedCandidate item : eligible) scores.add(item.score);
        RankedCandidate picked = eligible.get(exploration.chooseIndex(scores, snapshot.getExplorationEvidence(), input.getRequestId()));
        return new NextPreviewOutputData(input.getRequestId(), SELECTED,
                Optional.of(new SelectedCandidateData(picked.candidate.getPublicProfile(), snapshot.getFeedRevision(), date, picked.distance)));
    }

    private static final class RankedCandidate {
        private final CandidateSnapshot candidate;
        private final double score;
        private final OptionalInt distance;

        private RankedCandidate(CandidateSnapshot candidate, double score, OptionalInt distance) {
            this.candidate = candidate;
            this.score = score;
            this.distance = distance;
        }
    }
    private NextPreviewOutputData outcome(NextPreviewInputData input, NextPreviewOutputData.Status status) {
        return new NextPreviewOutputData(input.getRequestId(), status, Optional.empty());
    }
}
