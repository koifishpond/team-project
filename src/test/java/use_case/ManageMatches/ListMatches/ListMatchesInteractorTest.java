package use_case.ManageMatches.ListMatches;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Match;

class ListMatchesInteractorTest {
    private static final int ALICE = 100000001;
    private static final int BOB = 100000002;
    private static final int CARA = 100000003;
    private static final int DAN = 100000004;

    private FakeDataAccess dataAccess;
    private CapturingPresenter presenter;
    private ListMatchesInteractor interactor;

    @BeforeEach
    void setUp() {
        dataAccess = new FakeDataAccess();
        dataAccess.names.put(ALICE, "Alice");
        dataAccess.names.put(BOB, "Bob");
        dataAccess.names.put(CARA, "Cara");
        dataAccess.names.put(DAN, "Dan");
        presenter = new CapturingPresenter();
        interactor = new ListMatchesInteractor(dataAccess, presenter);
    }

    @Test
    void showsActiveMatchesNewestFirst() {
        dataAccess.matches.add(new Match(ALICE, BOB, Instant.parse("2026-11-01T10:00:00Z")));
        dataAccess.matches.add(new Match(CARA, ALICE, Instant.parse("2026-11-05T10:00:00Z")));

        interactor.execute(new ListMatchesInputData(ALICE));

        List<MatchSummary> result = presenter.output.getMatches();
        assertEquals(2, result.size());
        assertEquals("Cara", result.get(0).getDisplayName());
        assertEquals("Bob", result.get(1).getDisplayName());
        assertNull(presenter.error);
    }

    @Test
    void leavesOutEndedMatches() {
        Match ended = new Match(ALICE, BOB, Instant.parse("2026-11-01T10:00:00Z"));
        ended.end();
        dataAccess.matches.add(ended);

        interactor.execute(new ListMatchesInputData(ALICE));

        assertTrue(presenter.output.isEmpty());
    }

    @Test
    void leavesOutBlockedUsersInEitherDirection() {
        dataAccess.matches.add(new Match(ALICE, BOB, Instant.parse("2026-11-01T10:00:00Z")));
        dataAccess.matches.add(new Match(ALICE, CARA, Instant.parse("2026-11-02T10:00:00Z")));
        dataAccess.matches.add(new Match(ALICE, DAN, Instant.parse("2026-11-03T10:00:00Z")));
        dataAccess.blocks.add(ALICE + ">" + BOB);
        dataAccess.blocks.add(CARA + ">" + ALICE);

        interactor.execute(new ListMatchesInputData(ALICE));

        List<MatchSummary> result = presenter.output.getMatches();
        assertEquals(1, result.size());
        assertEquals(DAN, result.get(0).getOtherUserId());
    }

    @Test
    void noMatchesGivesAnEmptyList() {
        interactor.execute(new ListMatchesInputData(ALICE));

        assertTrue(presenter.output.isEmpty());
        assertNull(presenter.error);
    }

    @Test
    void unknownUserFails() {
        interactor.execute(new ListMatchesInputData(999999999));

        assertNull(presenter.output);
        assertEquals("User not found.", presenter.error);
    }

    private static final class FakeDataAccess implements ListMatchesDataAccessInterface {
        private final List<Match> matches = new ArrayList<>();
        private final Set<String> blocks = new HashSet<>();
        private final Map<Integer, String> names = new HashMap<>();

        @Override
        public List<Match> getMatchesFor(int userId) {
            List<Match> result = new ArrayList<>();
            for (Match match : matches) {
                if (match.involves(userId)) {
                    result.add(match);
                }
            }
            return result;
        }

        @Override
        public boolean hasBlocked(int blockerId, int blockedId) {
            return blocks.contains(blockerId + ">" + blockedId);
        }

        @Override
        public String getDisplayName(int userId) {
            return names.get(userId);
        }
    }

    private static final class CapturingPresenter implements ListMatchesOutputBoundary {
        private ListMatchesOutputData output;
        private String error;

        @Override
        public void prepareSuccessView(ListMatchesOutputData outputData) {
            output = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
