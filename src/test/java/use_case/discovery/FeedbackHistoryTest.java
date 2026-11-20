package use_case.discovery;

import entity.matchmaking.ProfileId;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static use_case.discovery.FeedbackEvent.Type.*;
import static use_case.discovery.FeedbackResult.*;

final class FeedbackHistoryTest {
    private static final String HIKING = "interest:HIKING", BOOKS = "interest:BOOKS", SMOKING = "smoking:NEVER";
    private static final Instant START = Instant.parse("2026-11-16T12:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 11, 16);
    private static final double EPSILON = 1e-12;
    private final ResolvedViewer viewer = new ResolvedViewer(new ProfileId(100000001), UUID.randomUUID(), UUID.randomUUID());
    private final ProfileId candidate = new ProfileId(100000002);
    private final FeedbackHistory history = new FeedbackHistory(2);

    @Test void engagementUsesOnlyValidatedCumulativeDwellAndDeliberateReactionReplacesIt() {
        UUID id = show(candidate, Set.of(HIKING));
        assertEquals(REJECTED, history.apply(viewer, FeedbackEvent.activeView(UUID.randomUUID(), id, 2, 0), START));
        assertTrue(history.authorizeDetail(viewer, id, Set.of(HIKING)));
        FeedbackEvent open = FeedbackEvent.profileOpened(UUID.randomUUID(), id, 3, Set.of(HIKING));
        assertEquals(APPLIED, history.apply(viewer, open, START));
        assertEquals(DUPLICATE, history.apply(viewer, open, START));
        assertEquals(0.05, affinity(HIKING), EPSILON);
        assertEquals(REJECTED, history.apply(viewer, FeedbackEvent.activeView(UUID.randomUUID(), id, 2, 0), START));
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.activeView(UUID.randomUUID(), id, 4, 30_000), START.plusSeconds(30)));
        assertEquals(0.10, affinity(HIKING), EPSILON);
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.activeView(UUID.randomUUID(), id, 5, 5_000), START.plusSeconds(31)));
        assertEquals(0.10, affinity(HIKING), EPSILON);
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.activeView(UUID.randomUUID(), id, 6, 600_000), START.plusSeconds(600)));
        assertEquals(0.15, affinity(HIKING), EPSILON);
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.reaction(UUID.randomUUID(), id, 7, SWIPE), START.plusSeconds(600)));
        assertEquals(-0.20, affinity(HIKING), EPSILON);
        assertEquals(REJECTED, history.apply(viewer, FeedbackEvent.activeView(UUID.randomUUID(), id, 8, 700_000), START.plusSeconds(700)));
        EngagementMetrics metrics = history.metrics(START.plusSeconds(700));
        assertEquals(1, metrics.firstOpens());
        assertEquals(600_000, metrics.activeMillis());
    }

    @Test void matchReplacesLikeByLinkAndOnlyCountsInsideTheFollowUpWindowForMatureRates() {
        UUID id = show(candidate, Set.of(HIKING, BOOKS));
        UUID like = UUID.randomUUID(), match = UUID.randomUUID();
        BackendMatchConfirmation late = new BackendMatchConfirmation(match, viewer.getViewerId(), candidate, id, like,
                START.plus(FeedbackHistory.FOLLOW_UP).plusSeconds(1));
        assertEquals(PENDING, history.confirm(late));
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.reaction(like, id, 2, LIKE), START));
        assertEquals(0.225, affinity(HIKING), EPSILON);
        assertEquals(REJECTED, history.apply(viewer, FeedbackEvent.reaction(like, id, 2, SWIPE), START));
        assertEquals(APPLIED, history.confirm(late));
        assertEquals(DUPLICATE, history.confirm(late));
        assertEquals(0.35, affinity(HIKING), EPSILON);
        assertEquals(0.35, affinity(BOOKS), EPSILON);
        assertEquals(1, history.metrics(late.confirmedAt()).confirmations());
        assertEquals(0, history.metrics(late.confirmedAt()).matureMatches());
    }

    @Test void hiddenFeaturesCannotLearnAndDetailEvidenceIsFrozen() {
        Set<String> visible = new HashSet<>(Set.of(HIKING));
        UUID id = show(candidate, visible);
        visible.clear();
        assertEquals(REJECTED, history.apply(viewer, FeedbackEvent.targetedDislike(UUID.randomUUID(), id, 2, SMOKING), START));
        assertTrue(history.authorizeDetail(viewer, id, Set.of(SMOKING, BOOKS)));
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.profileOpened(UUID.randomUUID(), id, 3, Set.of(SMOKING)), START));
        assertEquals(REJECTED, history.apply(viewer, FeedbackEvent.profileOpened(UUID.randomUUID(), id, 4, Set.of(BOOKS)), START));
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.targetedDislike(UUID.randomUUID(), id, 5, SMOKING), START));
        assertEquals(Map.of(SMOKING, -0.90), history.affinities());
    }

    @Test void rollingWindowEvictsDistinctCandidatesAndLateMatchesDoNotReinsertThem() {
        UUID id = show(candidate, Set.of(HIKING));
        UUID like = UUID.randomUUID();
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.reaction(like, id, 2, LIKE), START));
        act(show(new ProfileId(100000003), Set.of(BOOKS)), RECOMMEND_LESS);
        act(show(new ProfileId(100000004), Set.of("interest:ART")), DISLIKE);
        assertEquals(Map.of(BOOKS, -0.35, "interest:ART", -0.90), history.affinities());
        assertEquals(APPLIED, history.confirm(new BackendMatchConfirmation(UUID.randomUUID(), viewer.getViewerId(), candidate, id, like, START)));
        assertFalse(history.affinities().containsKey(HIKING));
        assertEquals(1, history.metrics(START).confirmations());
        assertEquals(3, history.metrics(START).impressions());
    }

    @Test void repeatedCandidateKeepsOneEffectiveContributionAndResetLeavesMetricsIntact() {
        UUID first = show(candidate, Set.of(HIKING));
        UUID like = UUID.randomUUID();
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.reaction(like, first, 2, LIKE), START));
        UUID second = show(candidate, Set.of(BOOKS));
        assertTrue(history.authorizeDetail(viewer, second, Set.of(BOOKS)));
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.profileOpened(UUID.randomUUID(), second, 2, Set.of(BOOKS)), START));
        assertEquals(0.45, affinity(HIKING), EPSILON);
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.reaction(UUID.randomUUID(), second, 3, RECOMMEND_LESS), START));
        assertEquals(Map.of(BOOKS, -0.35), history.affinities());
        history.resetLearning();
        assertEquals(APPLIED, history.confirm(new BackendMatchConfirmation(UUID.randomUUID(), viewer.getViewerId(), candidate, first, like, START)));
        assertTrue(history.affinities().isEmpty());
        assertEquals(2, history.metrics(START).impressions());
        assertEquals(1, history.metrics(START).firstOpens());
        assertEquals(1, history.metrics(START).confirmations());
        assertTrue(history.explorationEvidence(START.plus(FeedbackHistory.FOLLOW_UP)).isEmpty());
    }

    @Test void feedbackRequiresItsOriginalViewerAndBothSessions() {
        UUID id = show(candidate, Set.of(HIKING));
        FeedbackEvent like = FeedbackEvent.reaction(UUID.randomUUID(), id, 2, LIKE);
        assertEquals(REJECTED, history.apply(new ResolvedViewer(candidate, viewer.getLoginSessionId(), viewer.getDiscoverySessionId()), like, START));
        assertEquals(REJECTED, history.apply(new ResolvedViewer(viewer.getViewerId(), UUID.randomUUID(), viewer.getDiscoverySessionId()), like, START));
        assertEquals(REJECTED, history.apply(new ResolvedViewer(viewer.getViewerId(), viewer.getLoginSessionId(), UUID.randomUUID()), like, START));
        assertTrue(history.affinities().isEmpty());
        assertEquals(APPLIED, history.apply(viewer, like, START));
    }

    @Test void noShownKeysMeansNoLearningEvenFromADislike() {
        act(show(candidate, Set.of()), DISLIKE);
        assertTrue(history.affinities().isEmpty());
        assertEquals(1,history.metrics(START).deliberateDislikes());
    }

    private double affinity(String key) { return history.affinities().get(key); }
    private void act(UUID id, FeedbackEvent.Type type) {
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.reaction(UUID.randomUUID(), id, 2, type), START));
    }
    private UUID show(ProfileId id, Set<String> shown) {
        UUID exposure = UUID.randomUUID();
        assertTrue(history.capture(viewer, new FeedbackExposure(exposure, id, 3, 3, DATE, shown)));
        assertEquals(APPLIED, history.apply(viewer, FeedbackEvent.previewShown(UUID.randomUUID(), exposure, 1, shown), START));
        return exposure;
    }
}
