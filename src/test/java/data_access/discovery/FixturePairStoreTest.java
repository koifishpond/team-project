package data_access.discovery;

import entity.matchmaking.PairState;
import entity.matchmaking.ProfileId;
import org.junit.jupiter.api.Test;
import use_case.discovery.FeedbackEvent;
import use_case.discovery.ResolvedViewer;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class FixturePairStoreTest {
    private final FixturePairStore store = new FixturePairStore();
    private final ProfileId alex = new ProfileId(100000001), taylor = new ProfileId(100000002);

    @Test void reseedingAuthoritativeFactsDoesNotEraseAnAcceptedPass() {
        ResolvedViewer initial = session(alex);
        store.seed(initial, taylor, PairState.clear());
        store.apply(initial, taylor, FeedbackEvent.Type.SWIPE);
        store.seed(initial, taylor, PairState.clear());
        assertTrue(store.read(initial, taylor).orElseThrow().suppressedInCurrentSession());
        assertFalse(store.read(session(alex), taylor).orElseThrow().suppressedInCurrentSession());
    }

    @Test void reversePairSharesBlocksAndMatchesButDoesNotInheritOutgoingDislike() {
        ResolvedViewer a = session(alex), b = session(taylor);
        store.seed(a, taylor, new PairState(true, false, false, false, false, false, false));
        assertTrue(store.read(b, alex).orElseThrow().candidateBlockedViewer());
        store.seed(a, taylor, PairState.clear());
        store.apply(a, taylor, FeedbackEvent.Type.DISLIKE);
        assertTrue(store.read(session(alex), taylor).orElseThrow().viewerDislikedCandidate());
        assertFalse(store.read(b, alex).orElseThrow().excludes());
        store.match(alex, taylor);
        assertTrue(store.read(b, alex).orElseThrow().matched());
    }

    private static ResolvedViewer session(ProfileId profile) {
        return new ResolvedViewer(profile, UUID.randomUUID(), UUID.randomUUID());
    }
}
