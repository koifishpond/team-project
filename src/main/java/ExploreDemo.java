
import data_access.discovery.InMemoryDiscoveryDataAccess;
import entity.matchmaking.PairState;
import entity.matchmaking.ProfileFields;
import entity.matchmaking.ProfileId;
import use_case.discovery.ProfileCommitResult;
import use_case.discovery.ResolvedViewer;
import view.Explore.ExploreView;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public class ExploreDemo {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Explore Demo");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            ExploreView exploreView = new ExploreView();
            window.setContentPane(exploreView);

            Clock clock = Clock.system(ZoneId.of("America/Toronto"));
            InMemoryDiscoveryDataAccess data =
                    new InMemoryDiscoveryDataAccess(clock);

            UUID viewerToken = UUID.randomUUID();

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

            ExploreIntegration.connect(exploreView, viewerToken, data, clock);

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
