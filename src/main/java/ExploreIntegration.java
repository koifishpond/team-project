import interface_adapter.discovery.InteractionPresenter;
import use_case.discovery.RecordFeedbackInteractor;
import use_case.discovery.OpenProfileInteractor;

import interface_adapter.discovery.DiscoveryController;
import use_case.discovery.NextPreviewInteractor;
import use_case.discovery.EligibilityPolicy;
import use_case.discovery.ViewerScorer;
import use_case.discovery.ExplorationPolicy;

import use_case.discovery.NextPreviewOutputBoundary;

import interface_adapter.discovery.DiscoveryPresenter;
import use_case.discovery.FeedbackEvent;
import javax.swing.SwingUtilities;

import data_access.discovery.InMemoryDiscoveryDataAccess;
import java.time.Clock;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import use_case.discovery.SelectedCandidateData;
import interface_adapter.discovery.DiscoveryInteractionController;

import view.Explore.ExploreView;

public class ExploreIntegration {

    public static void connect(
            ExploreView exploreView,
            UUID viewerToken,
            InMemoryDiscoveryDataAccess data,
            Clock clock) {
        AtomicReference<SelectedCandidateData> currentCandidate =
                new AtomicReference<>();

        AtomicReference<UUID> currentExposure =
                new AtomicReference<>();

        AtomicLong feedbackSequence = new AtomicLong(0);

        AtomicReference<DiscoveryInteractionController> interactionRef =
                new AtomicReference<>();

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

            currentExposure.set(null);

            controller.next(UUID.randomUUID(), viewerToken);
        });

        controller.next(UUID.randomUUID(), viewerToken);

    }
}