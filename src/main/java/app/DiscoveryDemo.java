package app;

import data_access.discovery.InMemoryDiscoveryDataAccess;
import entity.matchmaking.*;
import interface_adapter.discovery.*;
import use_case.discovery.*;
import java.time.*;
import java.util.*;

/** Headless read-only selection driver, separate from the team's Swing Main. */
public final class DiscoveryDemo {
    private DiscoveryDemo() { }
    public static void main(String[] args) {
        Clock clock = Clock.system(ZoneId.of("America/Toronto"));
        InMemoryDiscoveryDataAccess data = new InMemoryDiscoveryDataAccess(clock);
        UUID viewerToken = UUID.randomUUID(), taylorToken = UUID.randomUUID(), morganToken = UUID.randomUUID();
        ProfileId viewerId = new ProfileId(100000001), taylorId = new ProfileId(100000002), morganId = new ProfileId(100000003);
        ResolvedViewer viewer = new ResolvedViewer(viewerId, UUID.randomUUID(), UUID.randomUUID());
        data.addSession(viewerToken, viewer);
        data.addSession(taylorToken, new ResolvedViewer(taylorId, UUID.randomUUID(), UUID.randomUUID()));
        data.addSession(morganToken, new ResolvedViewer(morganId, UUID.randomUUID(), UUID.randomUUID()));
        seed(data, viewerToken, viewerId, "Alex", "TORONTO", List.of("BOOKS"), true);
        seed(data, taylorToken, taylorId, "Taylor", "TORONTO", List.of("BOOKS", "HIKING"), false);
        seed(data, morganToken, morganId, "Morgan", "MISSISSAUGA", List.of("ART", "HIKING"), false);
        data.setPairState(viewer, taylorId, PairState.clear());
        data.setPairState(viewer, morganId, PairState.clear());
        List<SelectedCandidateData> shownCards = new ArrayList<>();
        DiscoveryPresenter presenter = new DiscoveryPresenter(model -> {
            System.out.println(model.getStatus() + ": " + model.getMessage());
            model.getCard().ifPresent(card -> System.out.println(card.getDisplayName() + ", " + card.getAgeYears() + " | "
                    + card.getDistanceLabel().orElse("Distance unknown") + " | " + card.getInterestLabels()));
        });
        NextPreviewOutputBoundary output = result -> {
            presenter.present(result);
            result.getSelectedCandidate().ifPresent(shownCards::add);
        };
        DiscoveryController controller = new DiscoveryController(new NextPreviewInteractor(data, data, output,
                new EligibilityPolicy(), new ViewerScorer(), clock));
        System.out.println("Before feedback:");
        controller.next(UUID.randomUUID(), viewerToken);
        SelectedCandidateData first = shownCards.get(0);
        UUID exposure = UUID.randomUUID();
        if (!first.getProfileId().equals(taylorId) || !data.captureExposure(viewerToken, exposure, first,
                Set.of("age:" + first.getAgeYears(), "distance:0", "interest:BOOKS", "interest:HIKING"))) {
            throw new IllegalStateException("Could not capture the first fixture card");
        }
        FeedbackResult result = new RecordFeedbackInteractor(data).execute(viewerToken,
                FeedbackEvent.targetedDislike(UUID.randomUUID(), exposure, "distance:0"));
        if (result != FeedbackResult.APPLIED) throw new IllegalStateException("Fixture feedback: " + result);
        System.out.println("After distance feedback (same eligible pool):");
        controller.next(UUID.randomUUID(), viewerToken);
        if (!shownCards.get(1).getProfileId().equals(morganId)) throw new IllegalStateException("Expected feedback to rerank the pool");
    }
    private static void seed(InMemoryDiscoveryDataAccess data, UUID token, ProfileId id, String name, String area,
                             List<String> interests, boolean isViewer) {
        requireSuccess(data.createDraft(token));
        ProfileFields.Input input = new ProfileFields.Input();
        input.displayName = name; input.dateOfBirth = LocalDate.of(1998, 4, 12); input.audience = "ANY";
        input.locationArea = area;
        input.interestTags = interests;
        if (isViewer) {
            input.preferredInterestTags = List.of("HIKING");
            input.distanceMode = "SOFT";
            input.maxKm = 10;
        }
        requireSuccess(data.commit(token, id, 1, InMemoryDiscoveryDataAccess.Operation.SAVE, input));
        requireSuccess(data.commit(token, id, 2, InMemoryDiscoveryDataAccess.Operation.PUBLISH, null));
    }
    private static void requireSuccess(ProfileCommitResult result) {
        if (result.getStatus() != ProfileCommitResult.Status.SUCCESS) throw new IllegalStateException("Fixture setup: " + result.getStatus());
    }
}
