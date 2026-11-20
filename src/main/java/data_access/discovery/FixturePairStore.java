package data_access.discovery;

import entity.matchmaking.PairState;
import entity.matchmaking.ProfileId;
import use_case.discovery.FeedbackEvent;
import use_case.discovery.ResolvedViewer;

import java.util.*;

/** Accessed under the enclosing fixture transaction. Permanent facts never use session IDs. */
final class FixturePairStore {
    private final Map<List<ProfileId>, PairState> baseline = new HashMap<>();
    private final Set<List<ProfileId>> likes = new HashSet<>();
    private final Set<List<ProfileId>> dislikes = new HashSet<>();
    private final Set<List<ProfileId>> matches = new HashSet<>();
    private final Set<List<Object>> seededSuppression = new HashSet<>();
    private final Set<List<Object>> acceptedSuppression = new HashSet<>();

    void seed(ResolvedViewer viewer, ProfileId candidate, PairState state) {
        baseline.put(directed(viewer.getViewerId(), candidate), new PairState(state.viewerBlockedCandidate(),
                state.candidateBlockedViewer(), state.viewerLikedCandidate(), state.matched(), state.viewerDislikedCandidate(),
                state.viewerReportedCandidate(), false));
        List<Object> session = sessionPair(viewer, candidate);
        if (state.suppressedInCurrentSession()) seededSuppression.add(session);
        else seededSuppression.remove(session);
    }

    Optional<PairState> read(ResolvedViewer viewer, ProfileId candidate) {
        List<ProfileId> key = directed(viewer.getViewerId(), candidate);
        List<ProfileId> reverse = directed(candidate, viewer.getViewerId());
        PairState local = baseline.get(key);
        PairState remote = baseline.get(reverse);
        if (local == null && remote == null) return Optional.empty();
        if (local == null) local = PairState.clear();
        if (remote == null) remote = PairState.clear();
        List<Object> session = sessionPair(viewer, candidate);
        return Optional.of(new PairState(local.viewerBlockedCandidate() || remote.candidateBlockedViewer(),
                local.candidateBlockedViewer() || remote.viewerBlockedCandidate(), local.viewerLikedCandidate() || likes.contains(key),
                local.matched() || remote.matched() || matches.contains(unordered(viewer.getViewerId(), candidate)),
                local.viewerDislikedCandidate() || dislikes.contains(key), local.viewerReportedCandidate(),
                seededSuppression.contains(session) || acceptedSuppression.contains(session)));
    }

    void apply(ResolvedViewer viewer, ProfileId candidate, FeedbackEvent.Type action) {
        List<ProfileId> key = directed(viewer.getViewerId(), candidate);
        switch (action) {
            case LIKE: like(viewer.getViewerId(), candidate); break;
            case DISLIKE: dislikes.add(key); break;
            case SWIPE: case RECOMMEND_LESS: case CLOSE: acceptedSuppression.add(sessionPair(viewer, candidate)); break;
            default: break;
        }
    }

    void like(ProfileId viewer, ProfileId candidate) { likes.add(directed(viewer, candidate)); }

    void match(ProfileId a, ProfileId b) { matches.add(unordered(a, b)); }

    static List<ProfileId> unordered(ProfileId a, ProfileId b) {
        return a.compareTo(b) < 0 ? List.of(a, b) : List.of(b, a);
    }

    private static List<ProfileId> directed(ProfileId a, ProfileId b) { return List.of(a, b); }

    private static List<Object> sessionPair(ResolvedViewer viewer, ProfileId candidate) {
        return List.of(viewer.getViewerId(), candidate, viewer.getDiscoverySessionId());
    }
}
