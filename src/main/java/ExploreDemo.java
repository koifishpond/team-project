import java.util.concurrent.atomic.AtomicLong;

import use_case.discovery.FeedbackEvent;

import use_case.discovery.SelectedCandidateData;
import use_case.discovery.NextPreviewOutputBoundary;
import java.util.concurrent.atomic.AtomicReference;

import interface_adapter.discovery.DiscoveryInteractionController;
import interface_adapter.discovery.InteractionPresenter;
import use_case.discovery.RecordFeedbackInteractor;
import use_case.discovery.OpenProfileInteractor;

import interface_adapter.discovery.DiscoveryController;
import use_case.discovery.NextPreviewInteractor;
import use_case.discovery.EligibilityPolicy;
import use_case.discovery.ViewerScorer;
import use_case.discovery.ExplorationPolicy;

import entity.matchmaking.ProfileFields;
import entity.matchmaking.PairState;
import use_case.discovery.ProfileCommitResult;
import java.time.LocalDate;
import java.util.List;

import entity.matchmaking.ProfileId;
import use_case.discovery.ResolvedViewer;

import data_access.discovery.InMemoryDiscoveryDataAccess;
import java.time.Clock;
import java.time.ZoneId;
import java.util.UUID;

import interface_adapter.discovery.DiscoveryPresenter;
import javax.swing.SwingUtilities;

import view.Explore.ExploreView;

import javax.swing.JFrame;

public class ExploreDemo {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            JFrame window = new JFrame("Explore Demo");

            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            ExploreView exploreView = new ExploreView();
            window.setContentPane(exploreView);

            AtomicReference<SelectedCandidateData> currentCandidate =
                    new AtomicReference<>();

            AtomicReference<UUID> currentExposure =
                    new AtomicReference<>();

            AtomicLong feedbackSequence = new AtomicLong(0);

            AtomicReference<DiscoveryInteractionController> interactionRef =
                    new AtomicReference<>();

            UUID viewerToken = UUID.randomUUID();

            // With DiscoveryPresenter
            DiscoveryPresenter presenter = new DiscoveryPresenter(
                    model -> {
                        System.out.println(model.getStatus() + ": " + model.getMessage());

                        SwingUtilities.invokeLater(() -> {
                            exploreView.displayDiscoveryResult(model);

                            SelectedCandidateData candidate = currentCandidate.get();
                            UUID exposureId = currentExposure.get();

                            if (model.getCard().isPresent()
                                    && candidate != null
                                    && exposureId != null
                                    && interactionRef.get() != null) {

                                interactionRef.get().interact(
                                        viewerToken,
                                        FeedbackEvent.previewShown(
                                                UUID.randomUUID(),
                                                exposureId,
                                                feedbackSequence.incrementAndGet(),
                                                candidate.getExposureKeys()
                                        )
                                );

                                System.out.println("PREVIEW_SHOWN sent");
                            }
                        });
                    }
            );

            Clock clock = Clock.system(ZoneId.of("America/Toronto"));

            InMemoryDiscoveryDataAccess data =
                    new InMemoryDiscoveryDataAccess(clock);

            ProfileId viewerId = new ProfileId(100000001);

            ResolvedViewer viewer = new ResolvedViewer(
                    viewerId,
                    UUID.randomUUID(),
                    UUID.randomUUID()
            );

            data.addSession(viewerToken, viewer);

            UUID taylorToken = UUID.randomUUID();
            UUID morganToken = UUID.randomUUID();
            UUID caseyToken = UUID.randomUUID();

            ProfileId taylorId = new ProfileId(100000002);
            ProfileId morganId = new ProfileId(100000003);
            ProfileId caseyId = new ProfileId(100000004);

            data.addSession(taylorToken,
                    new ResolvedViewer(taylorId, UUID.randomUUID(), UUID.randomUUID()));

            data.addSession(morganToken,
                    new ResolvedViewer(morganId, UUID.randomUUID(), UUID.randomUUID()));

            data.addSession(caseyToken,
                    new ResolvedViewer(caseyId, UUID.randomUUID(), UUID.randomUUID()));

            seed(data, viewerToken, viewerId, "Alex", "TORONTO",
                    List.of("BOOKS"), true);

            seed(data, taylorToken, taylorId, "Taylor", "TORONTO",
                    List.of("BOOKS", "HIKING"), false);

            seed(data, morganToken, morganId, "Morgan", "MISSISSAUGA",
                    List.of("ART", "HIKING"), false);

            seed(data, caseyToken, caseyId, "Casey", "TORONTO",
                    List.of("HIKING"), false);

            data.setPairState(viewer, taylorId, PairState.clear());
            data.setPairState(viewer, morganId, PairState.clear());
            data.setPairState(viewer, caseyId, PairState.clear());

            NextPreviewOutputBoundary output = result -> {
                SelectedCandidateData candidate =
                        result.getSelectedCandidate().orElse(null);

                currentCandidate.set(candidate);
                currentExposure.set(null);

                feedbackSequence.set(0);

                if (candidate != null) {
                    UUID exposureId = UUID.randomUUID();

                    if (data.preparePreview(viewerToken, exposureId, candidate)) {
                        currentExposure.set(exposureId);
                        System.out.println("Preview prepared: " + exposureId);
                    } else {
                        System.out.println("Could not prepare preview");
                    }
                }

                presenter.present(result);
            };

            DiscoveryController controller = new DiscoveryController(
                    new NextPreviewInteractor(
                            data,
                            data,
                            output,
                            new EligibilityPolicy(),
                            new ViewerScorer(),
                            clock,
                            ExplorationPolicy.disabled()
                    )
            );

            DiscoveryInteractionController interactions =
                    new DiscoveryInteractionController(
                            new RecordFeedbackInteractor(
                                    data,
                                    new InteractionPresenter(
                                            result -> System.out.println(
                                                    "Feedback status: " + result.getStatus()
                                            )
                                    )
                            ),
                            new OpenProfileInteractor(
                                    data,
                                    result -> {
                                        System.out.println(
                                                "Open profile result: " + result.getStatus()
                                        );

                                        result.getDetail().ifPresent(detail -> {
                                            SwingUtilities.invokeLater(() -> {
                                                exploreView.showFullProfile(
                                                        detail.getDisplayName(),
                                                        detail.getAgeYears(),
                                                        detail.getBio().orElse("No bio available"),
                                                        detail.getAreaLabel().orElse("Unknown"),
                                                        detail.getInterestTags().stream()
                                                                .map(tag -> tag.getLabel())
                                                                .collect(java.util.stream.Collectors.joining(", "))
                                                );
                                                UUID exposureId = currentExposure.get();

                                                if (exposureId != null
                                                        && exposureId.equals(detail.getExposureId())) {

                                                    interactionRef.get().interact(
                                                            viewerToken,
                                                            FeedbackEvent.profileOpened(
                                                                    UUID.randomUUID(),
                                                                    exposureId,
                                                                    feedbackSequence.incrementAndGet(),
                                                                    detail.getExposureKeys()
                                                            )
                                                    );

                                                    System.out.println("PROFILE_OPENED sent");
                                                }
                                            });
                                        });
                                    }
                            )
                    );

            interactionRef.set(interactions);

            exploreView.setInteractionListener(action -> {
                SelectedCandidateData candidate = currentCandidate.get();
                UUID exposureId = currentExposure.get();

                if (candidate == null || exposureId == null) {
                    System.out.println("No active candidate.");
                    return;
                }

                if ("OPEN_PROFILE".equals(action)) {
                    interactions.open(
                            UUID.randomUUID(),
                            viewerToken,
                            exposureId
                    );
                    return;
                }

                FeedbackEvent.Type type;

                if ("LIKE".equals(action)) {
                    type = FeedbackEvent.Type.LIKE;
                } else if ("PASS".equals(action)) {
                    type = FeedbackEvent.Type.SWIPE;
                } else {
                    return;
                }

                interactions.interact(
                        viewerToken,
                        FeedbackEvent.reaction(
                                UUID.randomUUID(),
                                exposureId,
                                feedbackSequence.incrementAndGet(),
                                type
                        )
                );

                // Prevent another reaction to the same candidate
                currentExposure.set(null);

                // Request the next candidate
                controller.next(UUID.randomUUID(), viewerToken);
            });

            controller.next(UUID.randomUUID(), viewerToken);

            window.setSize(900, 600);
            window.setLocationRelativeTo(null);
            window.setVisible(true);
        });
    }

    private static void seed(InMemoryDiscoveryDataAccess data,
                             UUID token, ProfileId id, String name,
                             String area, List<String> interests,
                             boolean isViewer) {

        requireSuccess(data.createDraft(token));

        ProfileFields.Input input = new ProfileFields.Input();
        input.displayName = name;
        input.dateOfBirth = LocalDate.of(1998, 4, 12);
        input.audience = "ANY";
        input.locationArea = area;
        input.interestTags = interests;

        if (isViewer) {
            input.preferredInterestTags = List.of("HIKING");
            input.distanceMode = "SOFT";
            input.maxKm = 10;
        }

        requireSuccess(data.commit(token, id, 1,
                InMemoryDiscoveryDataAccess.Operation.SAVE, input));

        requireSuccess(data.commit(token, id, 2,
                InMemoryDiscoveryDataAccess.Operation.PUBLISH, null));
    }

    private static void requireSuccess(ProfileCommitResult result) {
        if (result.getStatus() != ProfileCommitResult.Status.SUCCESS) {
            throw new IllegalStateException(
                    "Fixture setup: " + result.getStatus());
        }
    }
}
