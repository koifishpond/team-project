package app;

import data_access.discovery.InMemoryDiscoveryDataAccess;
import data_access.discovery.HttpProfileAssessmentDataAccess;
import entity.matchmaking.*;
import interface_adapter.discovery.*;
import use_case.discovery.*;
import java.time.*;
import java.util.*;

/** Headless fixture driver, separate from the team's Swing Main. */
public final class DiscoveryDemo {
    private DiscoveryDemo() { }
    public static void main(String[] args) {
        Clock clock = Clock.system(ZoneId.of("America/Toronto"));
        InMemoryDiscoveryDataAccess data = new InMemoryDiscoveryDataAccess(clock);
        UUID viewerToken = UUID.randomUUID(), taylorToken = UUID.randomUUID(), morganToken = UUID.randomUUID();
        ProfileId viewerId = new ProfileId(100000001), taylorId = new ProfileId(100000002), morganId = new ProfileId(100000003);
        ProfileId caseyId = new ProfileId(100000004);
        UUID caseyToken = UUID.randomUUID();
        ResolvedViewer viewer = new ResolvedViewer(viewerId, UUID.randomUUID(), UUID.randomUUID());
        data.addSession(viewerToken, viewer);
        data.addSession(taylorToken, new ResolvedViewer(taylorId, UUID.randomUUID(), UUID.randomUUID()));
        data.addSession(morganToken, new ResolvedViewer(morganId, UUID.randomUUID(), UUID.randomUUID()));
        data.addSession(caseyToken, new ResolvedViewer(caseyId, UUID.randomUUID(), UUID.randomUUID()));
        seed(data, viewerToken, viewerId, "Alex", "TORONTO", List.of("BOOKS"), true);
        seed(data, taylorToken, taylorId, "Taylor", "TORONTO", List.of("BOOKS", "HIKING"), false);
        seed(data, morganToken, morganId, "Morgan", "MISSISSAUGA", List.of("ART", "HIKING"), false);
        seed(data, caseyToken, caseyId, "Casey", "TORONTO", List.of("HIKING"), false);
        data.setPairState(viewer, taylorId, PairState.clear());
        data.setPairState(viewer, morganId, PairState.clear());
        data.setPairState(viewer, caseyId, PairState.clear());
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
                new EligibilityPolicy(), new ViewerScorer(), clock, ExplorationPolicy.disabled()));
        List<InteractionViewModel> receipts = new ArrayList<>();
        List<OpenProfileOutputData> details = new ArrayList<>();
        DiscoveryInteractionController interactions = new DiscoveryInteractionController(
                new RecordFeedbackInteractor(data, new InteractionPresenter(receipts::add)), new OpenProfileInteractor(data, details::add));
        System.out.println("Before feedback:");
        controller.next(UUID.randomUUID(), viewerToken);
        SelectedCandidateData first = shownCards.get(0);
        UUID exposure = UUID.randomUUID();
        if (!first.getProfileId().equals(taylorId) || !data.preparePreview(viewerToken, exposure, first)) {
            throw new IllegalStateException("Could not capture the first fixture card");
        }
        interactions.interact(viewerToken, FeedbackEvent.previewShown(UUID.randomUUID(), exposure, 1, first.getExposureKeys()));
        interactions.open(UUID.randomUUID(), viewerToken, exposure);
        PublicProfileDetailData detail = details.get(0).getDetail().orElseThrow();
        interactions.interact(viewerToken, FeedbackEvent.profileOpened(UUID.randomUUID(), exposure, 2, detail.getExposureKeys()));
        interactions.interact(viewerToken, FeedbackEvent.targetedDislike(UUID.randomUUID(), exposure, 3, "distance:0"));
        if (receipts.size() != 3 || receipts.stream().anyMatch(r -> r.getStatus() != InteractionOutputData.Status.ACCEPTED)) {
            throw new IllegalStateException("Fixture feedback failed");
        }
        System.out.println("After distance dislike (Taylor excluded, Morgan reranks above Casey):");
        controller.next(UUID.randomUUID(), viewerToken);
        if (!shownCards.get(1).getProfileId().equals(morganId)) throw new IllegalStateException("Expected feedback to rerank the pool");
        EngagementMetrics metrics = data.engagementMetrics(viewerId);
        System.out.println("Rendered impressions: " + metrics.impressions() + ", first opens: " + metrics.firstOpens());
        if (Arrays.asList(args).contains("--assess-synthetic")) {
            AssessmentResult assessment = new AssessProfileInteractor(HttpProfileAssessmentDataAccess.configured(System.getenv()))
                    .execute(new AssessmentInput(AssessmentInput.Fixture.DETAILED, 1));
            System.out.println("Synthetic assessment: " + assessment.status());
        }
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
