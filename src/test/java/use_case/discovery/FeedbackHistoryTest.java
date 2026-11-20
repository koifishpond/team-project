package use_case.discovery;

import entity.matchmaking.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static use_case.discovery.FeedbackEvent.Type.*;
import static use_case.discovery.FeedbackResult.*;

final class FeedbackHistoryTest {
    private static final String HIKING = "interest:HIKING";
    private static final String BOOKS = "interest:BOOKS";
    private static final String SMOKING = "smoking:NEVER";
    private static final String DISTANCE = "distance:5";
    private static final double EPSILON = 1e-12;
    private final FeedbackHistory history = new FeedbackHistory();
    private final ResolvedViewer viewer = new ResolvedViewer(new ProfileId(100000001), UUID.randomUUID(), UUID.randomUUID());
    private final ProfileId candidate = new ProfileId(100000002);

    @Test void engagementIsCumulativeBoundedAndReplacedByADeliberateReaction() {
        UUID shown = capture(candidate, Set.of(HIKING), Set.of(HIKING));
        FeedbackEvent open = FeedbackEvent.reaction(UUID.randomUUID(), shown, OPEN);
        assertEquals(APPLIED, apply(open));
        assertEquals(0.05, affinity(HIKING), EPSILON);
        assertEquals(DUPLICATE, apply(open));
        assertEquals(APPLIED, apply(FeedbackEvent.activeView(UUID.randomUUID(), shown, 30)));
        assertEquals(0.10, affinity(HIKING), EPSILON);
        assertEquals(APPLIED, apply(FeedbackEvent.activeView(UUID.randomUUID(), shown, 5)));
        assertEquals(0.10, affinity(HIKING), EPSILON);
        assertEquals(APPLIED, apply(FeedbackEvent.activeView(UUID.randomUUID(), shown, 600)));
        assertEquals(0.15, affinity(HIKING), EPSILON);
        assertEquals(APPLIED, apply(FeedbackEvent.reaction(UUID.randomUUID(), shown, SWIPE)));
        assertEquals(-0.20, affinity(HIKING), EPSILON);
        assertEquals(APPLIED, apply(FeedbackEvent.activeView(UUID.randomUUID(), shown, 700)));
        assertEquals(-0.20, affinity(HIKING), EPSILON);
    }

    @Test void likesHaveOneLinkedMatchUpgradeAcrossEventRetriesAndMatchIds() {
        UUID shown = capture(candidate, Set.of(HIKING, BOOKS), Set.of(HIKING, BOOKS));
        UUID likeId = UUID.randomUUID(), matchId = UUID.randomUUID(), eventId = UUID.randomUUID();
        FeedbackEvent match = FeedbackEvent.confirmedMatch(eventId, shown, likeId, matchId);
        assertEquals(REJECTED, apply(match));
        assertEquals(APPLIED, apply(FeedbackEvent.reaction(likeId, shown, LIKE)));
        assertEquals(0.225, affinity(HIKING), EPSILON);
        assertEquals(REJECTED, apply(FeedbackEvent.reaction(likeId, shown, SWIPE)));
        assertEquals(REJECTED, apply(FeedbackEvent.confirmedMatch(UUID.randomUUID(), shown, UUID.randomUUID(), matchId)));
        assertEquals(APPLIED, apply(match));
        assertEquals(0.35, affinity(HIKING), EPSILON);
        assertEquals(0.35, affinity(BOOKS), EPSILON);
        assertEquals(DUPLICATE, apply(match));
        assertEquals(DUPLICATE, apply(FeedbackEvent.confirmedMatch(UUID.randomUUID(), shown, likeId, matchId)));
        assertEquals(REJECTED, apply(FeedbackEvent.confirmedMatch(UUID.randomUUID(), shown, likeId, UUID.randomUUID())));
        assertEquals(0.35, affinity(HIKING), EPSILON);
    }

    @Test void targetedDislikeNeedsKnownExposedEvidenceAndOnlyUpdatesItsCoordinate() {
        Set<String> shownFeatures = new HashSet<>(Set.of(HIKING, SMOKING, DISTANCE));
        UUID shown = capture(candidate, Set.of(HIKING, SMOKING), shownFeatures);
        shownFeatures.clear();
        assertEquals(REJECTED, apply(FeedbackEvent.targetedDislike(UUID.randomUUID(), shown, DISTANCE)));
        assertEquals(APPLIED, apply(FeedbackEvent.targetedDislike(UUID.randomUUID(), shown, SMOKING)));
        assertEquals(Map.of(SMOKING, -0.90), history.affinities());
    }

    @Test void newestDeliberateExposureReplacesThePairAndOlderEventsCannotRestoreIt() {
        UUID first = capture(candidate, Set.of(HIKING), Set.of(HIKING));
        UUID likeId = UUID.randomUUID();
        assertEquals(APPLIED, apply(FeedbackEvent.reaction(likeId, first, LIKE)));
        UUID latest = capture(candidate, Set.of(BOOKS), Set.of(BOOKS));
        assertEquals(APPLIED, apply(FeedbackEvent.reaction(UUID.randomUUID(), latest, OPEN)));
        assertEquals(0.45, affinity(HIKING), EPSILON);
        assertEquals(APPLIED, apply(FeedbackEvent.reaction(UUID.randomUUID(), latest, RECOMMEND_LESS)));
        assertEquals(Map.of(BOOKS, -0.35), history.affinities());
        assertEquals(APPLIED, apply(FeedbackEvent.confirmedMatch(UUID.randomUUID(), first, likeId, UUID.randomUUID())));
        assertEquals(Map.of(BOOKS, -0.35), history.affinities());
    }

    @Test void unknownOrUnshownFeaturesCannotLearnFromGenericDislike() {
        UUID first = capture(candidate, Set.of(HIKING), Set.of(BOOKS));
        assertEquals(APPLIED, apply(FeedbackEvent.reaction(UUID.randomUUID(), first, DISLIKE)));
        assertTrue(history.affinities().isEmpty());
        UUID next = capture(candidate, Set.of(HIKING, BOOKS), Set.of(BOOKS, SMOKING));
        assertEquals(APPLIED, apply(FeedbackEvent.reaction(UUID.randomUUID(), next, DISLIKE)));
        assertEquals(Map.of(BOOKS, -0.90), history.affinities());
    }

    @Test void feedbackNeedsTheViewerAndBothSessionsThatCapturedTheExposure() {
        UUID shown = capture(candidate, Set.of(HIKING), Set.of(HIKING));
        FeedbackEvent event = FeedbackEvent.reaction(UUID.randomUUID(), shown, LIKE);
        assertEquals(REJECTED, history.apply(new ResolvedViewer(candidate, viewer.getLoginSessionId(), viewer.getDiscoverySessionId()), event));
        assertEquals(REJECTED, history.apply(new ResolvedViewer(viewer.getViewerId(), UUID.randomUUID(), viewer.getDiscoverySessionId()), event));
        assertEquals(REJECTED, history.apply(new ResolvedViewer(viewer.getViewerId(), viewer.getLoginSessionId(), UUID.randomUUID()), event));
        assertTrue(history.affinities().isEmpty());
        assertEquals(APPLIED, apply(event));
    }

    private FeedbackResult apply(FeedbackEvent event) { return history.apply(viewer, event); }
    private double affinity(String key) { return history.affinities().get(key); }

    private UUID capture(ProfileId id, Set<String> values, Set<String> shown) {
        UUID exposure = UUID.randomUUID();
        double[] attributes = new double[DiscoveryFeatures.dimension()];
        BitSet known = new BitSet();
        for (String key : values) {
            int index = DiscoveryFeatures.indexOf(key);
            attributes[index] = 1;
            known.set(index);
        }
        assertTrue(history.capture(viewer, new FeedbackExposure(exposure, id,
                new CandidateAttributeVector(1, attributes, known), shown)));
        return exposure;
    }
}
