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
        UUID viewerToken = UUID.randomUUID(), candidateToken = UUID.randomUUID();
        ProfileId viewerId = new ProfileId(100000001), candidateId = new ProfileId(100000002);
        ResolvedViewer viewer = new ResolvedViewer(viewerId, UUID.randomUUID(), UUID.randomUUID());
        data.addSession(viewerToken, viewer);
        data.addSession(candidateToken, new ResolvedViewer(candidateId, UUID.randomUUID(), UUID.randomUUID()));
        seed(data, viewerToken, viewerId, "Alex");
        seed(data, candidateToken, candidateId, "Taylor");
        data.setPairState(viewer, candidateId, PairState.clear());
        DiscoveryPresenter presenter = new DiscoveryPresenter(model -> {
            System.out.println(model.getStatus() + ": " + model.getMessage());
            model.getCard().ifPresent(card -> System.out.println(card.getDisplayName() + ", " + card.getAgeYears()
                    + " | " + card.getDistanceLabel().orElse("Distance unknown") + " | " + card.getInterestLabels()));
        });
        DiscoveryController controller = new DiscoveryController(new NextPreviewInteractor(data, data, presenter,
                new EligibilityPolicy(), new ViewerScorer(), clock));
        controller.next(UUID.randomUUID(), viewerToken);
    }
    private static void seed(InMemoryDiscoveryDataAccess data, UUID token, ProfileId id, String name) {
        requireSuccess(data.createDraft(token));
        ProfileFields.Input input = new ProfileFields.Input();
        input.displayName = name; input.dateOfBirth = LocalDate.of(1998, 4, 12); input.audience = "ANY";
        input.locationArea = "TORONTO"; input.interestTags = List.of("BOOKS", "HIKING");
        input.preferredInterestTags = List.of("HIKING");
        requireSuccess(data.commit(token, id, 1, InMemoryDiscoveryDataAccess.Operation.SAVE, input));
        requireSuccess(data.commit(token, id, 2, InMemoryDiscoveryDataAccess.Operation.PUBLISH, null));
    }
    private static void requireSuccess(ProfileCommitResult result) {
        if (result.getStatus() != ProfileCommitResult.Status.SUCCESS) throw new IllegalStateException("Fixture setup: " + result.getStatus());
    }
}
