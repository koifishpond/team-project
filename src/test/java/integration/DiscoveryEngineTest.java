package integration;

import data_access.discovery.InMemoryDiscoveryDataAccess;
import entity.matchmaking.*;
import interface_adapter.discovery.*;
import use_case.discovery.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static data_access.discovery.InMemoryDiscoveryDataAccess.Operation.*;
import static use_case.discovery.NextPreviewOutputData.Status.*;

/** Higher-level tests: real Controller, Interactor, fixture Data Access and plain Java Presenter. */
class DiscoveryEngineTest {
    private final TestClock clock=new TestClock(Instant.parse("2026-11-17T02:00:00Z"),ZoneOffset.UTC);
    private final LocalDate date=LocalDate.of(2026,11,16);
    private final InMemoryDiscoveryDataAccess data=new InMemoryDiscoveryDataAccess(clock);
    private final ProfileId viewerId=new ProfileId(100000001), candidateId=new ProfileId(100000002);
    private final UUID viewerToken=UUID.randomUUID(), candidateToken=UUID.randomUUID();
    private final ResolvedViewer viewer=new ResolvedViewer(viewerId,UUID.randomUUID(),UUID.randomUUID());
    private final List<DiscoveryViewModel> models=new ArrayList<>();
    private final List<NextPreviewOutputData> outputs=new ArrayList<>();
    private final List<InteractionViewModel> receipts=new ArrayList<>();
    private final List<OpenProfileOutputData> details=new ArrayList<>();
    private final DiscoveryPresenter presenter=new DiscoveryPresenter(models::add);
    private final DiscoveryController controller=new DiscoveryController(new NextPreviewInteractor(data,data,
            output->{outputs.add(output);presenter.present(output);},new EligibilityPolicy(),new ViewerScorer(),clock,ExplorationPolicy.disabled()));
    private final DiscoveryInteractionController interactions=new DiscoveryInteractionController(
            new RecordFeedbackInteractor(data,new InteractionPresenter(receipts::add)),new OpenProfileInteractor(data,details::add));

    DiscoveryEngineTest() {
        data.addSession(viewerToken,viewer);
        data.addSession(candidateToken,new ResolvedViewer(candidateId,UUID.randomUUID(),UUID.randomUUID()));
    }
    private ProfileFields.Input minimum(String name) {
        ProfileFields.Input in=new ProfileFields.Input(); in.displayName=name; in.dateOfBirth=date.minusYears(27); in.audience="ANY"; return in;
    }
    private DiscoveryProfile success(ProfileCommitResult result) {
        assertEquals(ProfileCommitResult.Status.SUCCESS,result.getStatus(),()->result.getFieldErrors().toString());
        return result.getProfile().orElseThrow();
    }
    private void publish(UUID token,ProfileId id,ProfileFields.Input fields) {
        success(data.createDraft(token)); success(data.commit(token,id,1,SAVE,fields)); success(data.commit(token,id,2,PUBLISH,null));
    }
    private void ready(ProfileFields.Input candidate) {
        publish(viewerToken,viewerId,minimum("Viewer")); publish(candidateToken,candidateId,candidate);
        data.setPairState(viewer,candidateId,PairState.clear());
    }
    private DiscoveryViewModel next(UUID token) {
        int before=models.size(); UUID request=UUID.randomUUID(); controller.next(request,token);
        assertEquals(before+1,models.size()); assertEquals(models.size(),outputs.size());
        DiscoveryViewModel model=models.get(before); assertEquals(request,model.getRequestId());
        assertEquals(request,outputs.get(before).getRequestId());
        assertEquals(model.getStatus()==SELECTED,model.getCard().isPresent()); return model;
    }
    @Test void minimumProfileSelectedWithPlaceholderAndNoOptionalRows() {
        ready(minimum("Taylor")); DiscoveryViewModel model=next(viewerToken);
        assertEquals(SELECTED,model.getStatus()); DiscoveryViewModel.Card card=model.getCard().orElseThrow();
        assertEquals(candidateId,card.getProfileId()); assertEquals(3,card.getProfileRevision());
        assertEquals("Taylor",card.getDisplayName()); assertEquals(27,card.getAgeYears());
        assertTrue(card.getPrimaryPhotoId().isEmpty()); assertTrue(card.getDistanceLabel().isEmpty());
        assertTrue(card.getBioExcerpt().isEmpty()); assertTrue(card.getInterestLabels().isEmpty());
    }
    @Test void publicPreviewFormatsUnicodeExcerptPhotoCatalogOrderAndCoarseDistance() {
        UUID photo=UUID.randomUUID(); data.registerPhoto(photo,candidateId);
        ProfileFields.Input c=minimum("  Taylor  "); c.photoIds=List.of(photo); c.locationArea="MISSISSAUGA";
        c.bio="  Hello\n\n  " + "😀".repeat(130); c.interestTags=List.of("HIKING","BOOKS","ART","MUSIC");
        c.gender="WOMAN"; c.showGender=false; c.smoking="NEVER"; c.drinking="REGULARLY"; c.relationshipIntent="LONG_TERM";
        c.preferredInterestTags=List.of("SPORTS"); c.minYears=18;c.maxYears=40;c.ageMode="SOFT";
        ready(c);
        ProfileFields.Input v=minimum("Viewer"); v.locationArea="TORONTO"; success(data.commit(viewerToken,viewerId,3,SAVE,v));
        DiscoveryViewModel.Card card=next(viewerToken).getCard().orElseThrow();
        assertEquals("Taylor",card.getDisplayName()); assertEquals(Optional.of(photo),card.getPrimaryPhotoId());
        assertEquals(List.of("Art","Books","Hiking"),card.getInterestLabels());
        assertEquals(Optional.of("Within 25 km"),card.getDistanceLabel());
        String excerpt=card.getBioExcerpt().orElseThrow(); assertEquals(120,excerpt.codePointCount(0,excerpt.length()));
        assertTrue(excerpt.startsWith("Hello ")); assertTrue(excerpt.endsWith("…")); assertFalse(excerpt.contains("\n"));
        SelectedCandidateData out=outputs.get(0).getSelectedCandidate().orElseThrow();
        assertEquals(OptionalInt.of(21967),out.getDistanceMetres()); assertEquals(date,out.getEffectiveDate());
        assertEquals(data.getFeedRevision(),out.getFeedRevision());
        assertThrows(UnsupportedOperationException.class,()->card.getInterestLabels().clear());
        assertThrows(UnsupportedOperationException.class,()->out.getPreviewInterestTags().clear());
    }
    @Test void repeatedRequestIsReadOnlyAndPreservesCorrelation() {
        ready(minimum("Taylor")); long feed=data.getFeedRevision();
        DiscoveryProfile before=data.getOwnedProfile(candidateToken,candidateId).orElseThrow();
        DiscoverySnapshot snapshot=data.load(viewer,date).getSnapshot().orElseThrow();
        UUID request=UUID.randomUUID(); controller.next(request,viewerToken); controller.next(request,viewerToken);
        assertEquals(2,outputs.size());
        for (NextPreviewOutputData out:outputs) {
            assertEquals(request,out.getRequestId()); assertEquals(SELECTED,out.getStatus());
            assertEquals(candidateId,out.getSelectedCandidate().orElseThrow().getProfileId());
            assertEquals(feed,out.getSelectedCandidate().orElseThrow().getFeedRevision());
        }
        assertEquals(feed,data.getFeedRevision()); assertSame(before,data.getOwnedProfile(candidateToken,candidateId).orElseThrow());
        assertFalse(data.load(viewer,date).getSnapshot().orElseThrow().getCandidates().get(0).getPairState().excludes());
        assertEquals(snapshot.getCandidates().get(0).getProfile().getProfileRevision(),before.getProfileRevision());
    }
    @Test void emptyExpiredUnavailableAndRetryableOutcomesUseRealAdapters() {
        assertEquals(VIEWER_UNAVAILABLE,next(viewerToken).getStatus());
        publish(viewerToken,viewerId,minimum("Viewer")); assertEquals(EMPTY,next(viewerToken).getStatus());
        publish(candidateToken,candidateId,minimum("Taylor"));
        assertEquals(RETRYABLE_FAILURE,next(viewerToken).getStatus()); // no authoritative pair state
        data.setPairState(viewer,candidateId,new PairState(true,false,false,false,false,false,false)); assertEquals(EMPTY,next(viewerToken).getStatus());
        data.setAvailable(false); DiscoveryViewModel failure=next(viewerToken); assertEquals(RETRYABLE_FAILURE,failure.getStatus()); assertTrue(failure.getMessage().contains("Retry"));
        data.setAvailable(true); data.expireSession(viewerToken); assertEquals(SESSION_EXPIRED,next(viewerToken).getStatus());
    }
    @Test void editsPauseResumeAndViewerAvailabilityAffectLaterDiscovery() {
        ready(minimum("Taylor")); DiscoverySnapshot old=data.load(viewer,date).getSnapshot().orElseThrow();
        assertEquals(SELECTED,next(viewerToken).getStatus()); long revision=data.getFeedRevision();
        ProfileFields.Input edit=minimum("Updated"); edit.interestTags=List.of("ART");
        success(data.commit(candidateToken,candidateId,3,SAVE,edit));
        assertEquals(revision+1,data.getFeedRevision()); assertEquals("Updated",next(viewerToken).getCard().orElseThrow().getDisplayName());
        assertEquals("Taylor",old.getCandidates().get(0).getPublicProfile().getDisplayName());
        success(data.commit(candidateToken,candidateId,4,PAUSE,null)); assertEquals(EMPTY,next(viewerToken).getStatus());
        edit.displayName=null; success(data.commit(candidateToken,candidateId,5,SAVE,edit));
        assertEquals(ProfileCommitResult.Status.VALIDATION_FAILED,data.commit(candidateToken,candidateId,6,RESUME,null).getStatus());
        assertEquals(EMPTY,next(viewerToken).getStatus());
        edit.displayName="Restored"; success(data.commit(candidateToken,candidateId,6,SAVE,edit));
        success(data.commit(candidateToken,candidateId,7,RESUME,null));
        assertEquals("Restored",next(viewerToken).getCard().orElseThrow().getDisplayName());
        success(data.commit(viewerToken,viewerId,3,PAUSE,null)); assertEquals(VIEWER_UNAVAILABLE,next(viewerToken).getStatus());
        success(data.commit(viewerToken,viewerId,4,RESUME,null)); assertEquals(SELECTED,next(viewerToken).getStatus());
    }
    @Test void committedHardRulesImmediatelyChangeEligibility() {
        ready(minimum("Taylor")); assertEquals(SELECTED,next(viewerToken).getStatus());
        ProfileFields.Input c=minimum("Taylor"); c.minYears=30;c.maxYears=40;c.ageMode="HARD";
        success(data.commit(candidateToken,candidateId,3,SAVE,c)); assertEquals(EMPTY,next(viewerToken).getStatus());
        c.ageMode="SOFT";success(data.commit(candidateToken,candidateId,4,SAVE,c)); assertEquals(SELECTED,next(viewerToken).getStatus());
    }

    @Test void targetedLearningReordersRemainingCandidatesAndHardRulesStillWin() {
        ProfileFields.Input alex=minimum("Alex"); alex.locationArea="TORONTO";
        alex.preferredInterestTags=List.of("HIKING"); alex.maxKm=10; alex.distanceMode="SOFT";
        ProfileFields.Input taylor=minimum("Taylor"); taylor.locationArea="TORONTO"; taylor.interestTags=List.of("HIKING");
        ProfileFields.Input morgan=minimum("Morgan"); morgan.locationArea="MISSISSAUGA"; morgan.interestTags=List.of("HIKING");
        publish(viewerToken,viewerId,alex); publish(candidateToken,candidateId,taylor);
        ProfileId morganId=new ProfileId(100000003); addCandidate(morganId,morgan);
        ProfileId caseyId=new ProfileId(100000004); addCandidate(caseyId,taylor);
        data.setPairState(viewer,candidateId,PairState.clear());
        assertEquals(candidateId,next(viewerToken).getCard().orElseThrow().getProfileId());
        long before=data.getFeedRevision();
        UUID exposure=show(viewerToken,Set.of("distance:0","interest:HIKING","age:27"));
        FeedbackEvent dislike=FeedbackEvent.targetedDislike(UUID.randomUUID(),exposure,2,"distance:0");
        assertEquals(InteractionOutputData.Status.REJECTED,send(candidateToken,dislike).getStatus());
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,dislike).getStatus());
        assertEquals(InteractionOutputData.Status.ALREADY_APPLIED,send(viewerToken,dislike).getStatus());
        assertEquals(before+2,data.getFeedRevision());
        DiscoverySnapshot snapshot=data.load(viewer,date).getSnapshot().orElseThrow();
        assertEquals(-0.4,affinity(snapshot,"distance:0"),1e-12);
        assertEquals(0.5,affinity(snapshot,"interest:HIKING"));
        assertEquals(0,affinity(snapshot,"age:27"));
        assertEquals(1,snapshot.getCandidates().stream().filter(candidate->candidate.getPairState().excludes()).count());
        assertEquals(morganId,next(viewerToken).getCard().orElseThrow().getProfileId());
        assertEquals(before+2,data.getFeedRevision());
        assertEquals(Set.of(InterestTag.HIKING),data.getOwnedProfile(viewerToken,viewerId).orElseThrow()
                .getFields().getPreferences().getPreferredInterestTags());
        alex.distanceMode="HARD";
        success(data.commit(viewerToken,viewerId,3,SAVE,alex));
        assertEquals(0.5,affinity(data.load(viewer,date).getSnapshot().orElseThrow(),"distance:0"));
        assertEquals(caseyId,next(viewerToken).getCard().orElseThrow().getProfileId());
        data.setPairState(viewer,caseyId,new PairState(false,true,false,false,false,false,false));
        assertEquals(EMPTY,next(viewerToken).getStatus());
    }

    @Test void acceptedLikeAndBackendMatchLearnFrozenEvidenceForOnlyTheOriginalViewer() {
        ProfileFields.Input taylor=minimum("Taylor"); taylor.interestTags=List.of("BOOKS");
        ready(taylor);
        ResolvedViewer reverse=data.resolve(candidateToken).getViewer().orElseThrow();
        data.setPairState(reverse,viewerId,PairState.clear());
        assertEquals(SELECTED,next(viewerToken).getStatus());
        SelectedCandidateData shown=lastSelection();
        UUID exposure=show(viewerToken,Set.of("interest:BOOKS"));
        assertFalse(data.preparePreview(viewerToken,exposure,shown));
        UUID likeId=UUID.randomUUID(), matchId=UUID.randomUUID();
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,FeedbackEvent.reaction(likeId,exposure,2,FeedbackEvent.Type.LIKE)).getStatus());
        assertEquals(EMPTY,next(viewerToken).getStatus());
        assertEquals(SELECTED,next(candidateToken).getStatus()); // incoming like does not exclude
        taylor.interestTags=List.of("ART"); taylor.smoking="NEVER";
        success(data.commit(candidateToken,candidateId,3,SAVE,taylor));
        assertFalse(data.preparePreview(viewerToken,UUID.randomUUID(),shown));
        ViewerPreferenceVector afterLike=data.load(viewer,date).getSnapshot().orElseThrow().getViewerPreferences();
        assertEquals(0.45,afterLike.affinityAt(DiscoveryFeatures.indexOf("interest:BOOKS")),1e-12);
        data.expireSession(viewerToken);
        ConfirmMatchInteractor backend=new ConfirmMatchInteractor(data);
        BackendMatchConfirmation match=new BackendMatchConfirmation(matchId,viewerId,candidateId,exposure,likeId,clock.instant());
        assertEquals(FeedbackResult.APPLIED,backend.execute(match));
        long afterMatch=data.getFeedRevision();
        assertEquals(FeedbackResult.DUPLICATE,backend.execute(match));
        assertEquals(afterMatch,data.getFeedRevision());
        assertEquals(EMPTY,next(candidateToken).getStatus());
        DiscoverySnapshot snapshot=data.load(viewer,date).getSnapshot().orElseThrow();
        assertEquals(0.70,affinity(snapshot,"interest:BOOKS"),1e-12);
        assertEquals(0,affinity(snapshot,"interest:ART"));
        assertEquals(0,affinity(snapshot,"smoking:NEVER"));
        assertEquals(0.45,afterLike.affinityAt(DiscoveryFeatures.indexOf("interest:BOOKS")),1e-12);
        assertEquals(0,affinity(data.load(reverse,date).getSnapshot().orElseThrow(),"interest:BOOKS"));
    }

    @Test void fixtureExposureAndFeedbackRespectSessionAndAvailability() {
        ready(minimum("Taylor"));
        UUID exposure=UUID.randomUUID();
        assertEquals(SELECTED,next(viewerToken).getStatus());
        SelectedCandidateData shown=lastSelection();
        data.setPairState(viewer,candidateId,new PairState(true,false,false,false,false,false,false));
        assertFalse(data.preparePreview(viewerToken,exposure,shown));
        data.setPairState(viewer,candidateId,PairState.clear());
        assertEquals(SELECTED,next(viewerToken).getStatus());
        assertTrue(data.preparePreview(viewerToken,exposure,lastSelection()));
        FeedbackEvent opened=FeedbackEvent.previewShown(UUID.randomUUID(),exposure,1,Set.of("age:27"));
        data.setAvailable(false);
        assertEquals(InteractionOutputData.Status.RETRYABLE_FAILURE,send(viewerToken,opened).getStatus());
        data.setAvailable(true); data.expireSession(viewerToken);
        assertEquals(InteractionOutputData.Status.SESSION_EXPIRED,send(viewerToken,opened).getStatus());
        assertEquals(0,affinity(data.load(viewer,date).getSnapshot().orElseThrow(),"age:27"));
    }

    @Test void swipesAndNeutralCloseSuppressOnlyForTheCurrentDiscoverySession() {
        ready(minimum("Taylor"));
        assertEquals(SELECTED,next(viewerToken).getStatus());
        UUID exposure=show(viewerToken,Set.of("age:27"));
        FeedbackEvent passed=FeedbackEvent.reaction(UUID.randomUUID(),exposure,2,FeedbackEvent.Type.SWIPE);
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,passed).getStatus());
        long revision=data.getFeedRevision();
        assertEquals(InteractionOutputData.Status.ALREADY_APPLIED,send(viewerToken,passed).getStatus());
        assertEquals(revision,data.getFeedRevision());
        assertEquals(EMPTY,next(viewerToken).getStatus());
        UUID resumed=newSession(viewer.getDiscoverySessionId());
        assertEquals(EMPTY,next(resumed).getStatus());
        UUID restarted=newSession(UUID.randomUUID());
        assertEquals(SELECTED,next(restarted).getStatus());
        UUID unshown=UUID.randomUUID();
        assertTrue(data.preparePreview(restarted,unshown,lastSelection()));
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(restarted,
                FeedbackEvent.reaction(UUID.randomUUID(),unshown,1,FeedbackEvent.Type.CLOSE)).getStatus());
        assertEquals(1,data.engagementMetrics(viewerId).impressions());
        assertEquals(SELECTED,next(restarted).getStatus());
        UUID shown=show(restarted,Set.of("age:27"));
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(restarted,
                FeedbackEvent.reaction(UUID.randomUUID(),shown,2,FeedbackEvent.Type.CLOSE)).getStatus());
        assertEquals(EMPTY,next(restarted).getStatus());
        assertEquals(-0.20,affinity(data.load(data.resolve(restarted).getViewer().orElseThrow(),date).getSnapshot().orElseThrow(),"age:27"),1e-12);
        assertEquals(1,data.engagementMetrics(viewerId).returnSessions());
    }

    @Test void pendingBackendMatchAttachesExactlyOnceAndExcludesBothPeopleAfterSessionRestart() {
        ProfileFields.Input c=minimum("Taylor"); c.interestTags=List.of("BOOKS"); ready(c);
        assertEquals(SELECTED,next(viewerToken).getStatus());
        UUID exposure=show(viewerToken,Set.of("interest:BOOKS")), like=UUID.randomUUID();
        ConfirmMatchInteractor backend=new ConfirmMatchInteractor(data);
        BackendMatchConfirmation early=new BackendMatchConfirmation(UUID.randomUUID(),viewerId,candidateId,exposure,like,clock.instant());
        assertEquals(FeedbackResult.PENDING,backend.execute(early));
        assertEquals(FeedbackResult.PENDING,backend.execute(early));
        assertEquals(0,data.engagementMetrics(viewerId).confirmations());
        long before=data.getFeedRevision();
        FeedbackEvent action=FeedbackEvent.reaction(like,exposure,2,FeedbackEvent.Type.LIKE);
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,action).getStatus());
        assertEquals(before+1,data.getFeedRevision());
        assertEquals(FeedbackResult.DUPLICATE,backend.execute(early));
        assertEquals(1,data.engagementMetrics(viewerId).likes());
        assertEquals(1,data.engagementMetrics(viewerId).confirmations());
        data.expireSession(viewerToken); data.expireSession(candidateToken);
        UUID newViewer=newSession(UUID.randomUUID()), newCandidate=UUID.randomUUID();
        data.addSession(newCandidate,new ResolvedViewer(candidateId,UUID.randomUUID(),UUID.randomUUID()));
        assertEquals(EMPTY,next(newViewer).getStatus()); assertEquals(EMPTY,next(newCandidate).getStatus());
        assertEquals(0.70,affinity(data.load(data.resolve(newViewer).getViewer().orElseThrow(),date).getSnapshot().orElseThrow(),"interest:BOOKS"),1e-12);
        assertEquals(FeedbackResult.REJECTED,backend.execute(new BackendMatchConfirmation(UUID.randomUUID(),viewerId,candidateId,
                exposure,like,clock.instant())));
    }

    @Test void nonAcceptedActionsAndMismatchedMatchLinksDoNotChangeEligibility() {
        ready(minimum("Taylor")); assertEquals(SELECTED,next(viewerToken).getStatus());
        UUID exposure=show(viewerToken,Set.of("age:27"));
        UUID expectedLike=UUID.randomUUID();
        ConfirmMatchInteractor backend=new ConfirmMatchInteractor(data);
        BackendMatchConfirmation pending=new BackendMatchConfirmation(UUID.randomUUID(),viewerId,candidateId,exposure,expectedLike,clock.instant());
        assertEquals(FeedbackResult.PENDING,backend.execute(pending));
        assertEquals(InteractionOutputData.Status.REJECTED,send(viewerToken,FeedbackEvent.reaction(expectedLike,exposure,1,FeedbackEvent.Type.LIKE)).getStatus());
        assertEquals(SELECTED,next(candidateToken).getStatus());
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,FeedbackEvent.reaction(UUID.randomUUID(),exposure,2,FeedbackEvent.Type.SWIPE)).getStatus());
        assertEquals(FeedbackResult.REJECTED,backend.execute(pending));
        assertEquals(0,data.engagementMetrics(viewerId).confirmations());
        assertEquals(SELECTED,next(candidateToken).getStatus());
    }

    @ParameterizedTest
    @ValueSource(booleans={true,false})
    void bothParticipantsGetOneUpgradeWithEitherBackendDeliveryOrder(boolean confirmationFirst) {
        ProfileFields.Input alex=minimum("Alex"), taylor=minimum("Taylor");
        alex.interestTags=List.of("ART","MUSIC"); taylor.interestTags=List.of("BOOKS","HIKING");
        publish(viewerToken,viewerId,alex); publish(candidateToken,candidateId,taylor);
        data.setPairState(viewer,candidateId,PairState.clear());
        assertEquals(candidateId,next(viewerToken).getCard().orElseThrow().getProfileId());
        UUID alexOffer=show(viewerToken,Set.of("interest:BOOKS"));
        assertEquals(viewerId,next(candidateToken).getCard().orElseThrow().getProfileId());
        UUID taylorOffer=show(candidateToken,Set.of("interest:ART"));
        FeedbackEvent alexLike=FeedbackEvent.reaction(UUID.randomUUID(),alexOffer,2,FeedbackEvent.Type.LIKE);
        FeedbackEvent taylorLike=FeedbackEvent.reaction(UUID.randomUUID(),taylorOffer,2,FeedbackEvent.Type.LIKE);
        BackendAcceptedLike replay=new BackendAcceptedLike(candidateId,viewerId,taylorLike,clock.instant());
        UUID matchId=UUID.randomUUID();
        BackendMatchConfirmation alexConfirmation=new BackendMatchConfirmation(matchId,viewerId,candidateId,alexOffer,alexLike.id(),clock.instant());
        BackendMatchConfirmation taylorConfirmation=new BackendMatchConfirmation(matchId,candidateId,viewerId,taylorOffer,taylorLike.id(),clock.instant());
        ConfirmMatchInteractor matches=new ConfirmMatchInteractor(data);
        ReplayAcceptedLikeInteractor likes=new ReplayAcceptedLikeInteractor(data);

        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,alexLike).getStatus());
        if (confirmationFirst) {
            assertEquals(FeedbackResult.PENDING,matches.execute(taylorConfirmation));
            assertEquals(FeedbackResult.PENDING,matches.execute(taylorConfirmation));
        }
        assertEquals(FeedbackResult.APPLIED,matches.execute(alexConfirmation));
        long beforeReplay=data.getFeedRevision();
        InteractionViewModel refused=send(candidateToken,taylorLike);
        assertEquals(InteractionOutputData.Status.REJECTED,refused.getStatus());
        assertEquals(InteractionOutputData.Rejection.PREVIEW_UNAVAILABLE,refused.getRejection().orElseThrow());
        assertEquals(beforeReplay,data.getFeedRevision());
        assertEquals(0,data.engagementMetrics(candidateId).likes());
        assertEquals(0,data.engagementMetrics(candidateId).confirmations());

        UUID taylorSession=candidateToken;
        if (confirmationFirst) {
            data.expireSession(candidateToken);
            assertEquals(InteractionOutputData.Status.SESSION_EXPIRED,send(candidateToken,taylorLike).getStatus());
            taylorSession=UUID.randomUUID();
            data.addSession(taylorSession,new ResolvedViewer(candidateId,UUID.randomUUID(),UUID.randomUUID()));
            assertEquals(InteractionOutputData.Status.REJECTED,send(taylorSession,taylorLike).getStatus());
            alex.interestTags=List.of("TRAVEL");
            success(data.commit(viewerToken,viewerId,3,SAVE,alex));
        }
        beforeReplay=data.getFeedRevision();
        assertEquals(FeedbackResult.APPLIED,likes.execute(replay));
        assertEquals(beforeReplay+1,data.getFeedRevision());
        assertEquals(FeedbackResult.DUPLICATE,likes.execute(replay));
        assertEquals(beforeReplay+1,data.getFeedRevision());
        if (!confirmationFirst) {
            assertEquals(0,data.engagementMetrics(candidateId).confirmations());
            DiscoverySnapshot snapshot=data.load(data.resolve(taylorSession).getViewer().orElseThrow(),date).getSnapshot().orElseThrow();
            assertEquals(0.45,affinity(snapshot,"interest:ART"),1e-12);
            assertEquals(FeedbackResult.APPLIED,matches.execute(taylorConfirmation));
        }
        long finalRevision=data.getFeedRevision();
        assertEquals(FeedbackResult.DUPLICATE,matches.execute(alexConfirmation));
        assertEquals(FeedbackResult.DUPLICATE,matches.execute(taylorConfirmation));
        assertEquals(FeedbackResult.DUPLICATE,likes.execute(new BackendAcceptedLike(viewerId,candidateId,alexLike,clock.instant())));
        assertEquals(finalRevision,data.getFeedRevision());
        assertEquals(EMPTY,next(viewerToken).getStatus());
        assertEquals(EMPTY,next(taylorSession).getStatus());
        assertEquals(1,data.engagementMetrics(viewerId).impressions());
        assertEquals(1,data.engagementMetrics(candidateId).impressions());
        for (ProfileId participant : List.of(viewerId,candidateId)) {
            assertEquals(1,data.engagementMetrics(participant).likes());
            assertEquals(1,data.engagementMetrics(participant).confirmations());
        }
        DiscoverySnapshot alexSnapshot=data.load(viewer,date).getSnapshot().orElseThrow();
        DiscoverySnapshot taylorSnapshot=data.load(data.resolve(taylorSession).getViewer().orElseThrow(),date).getSnapshot().orElseThrow();
        assertEquals(0.70,affinity(alexSnapshot,"interest:BOOKS"),1e-12);
        assertEquals(0.70,affinity(taylorSnapshot,"interest:ART"),1e-12);
        assertEquals(0,affinity(alexSnapshot,"interest:HIKING"));
        assertEquals(0,affinity(taylorSnapshot,"interest:MUSIC"));
        assertEquals(0,affinity(taylorSnapshot,"interest:TRAVEL"));
        clock.advance(Duration.ofHours(24));
        assertEquals(1,data.engagementMetrics(viewerId).matureMatches());
        assertEquals(1,data.engagementMetrics(candidateId).matureMatches());
    }

    @Test void conflictingBackendLinksCannotConsumeTheCanonicalPendingMatch() {
        ready(minimum("Taylor"));
        assertEquals(SELECTED,next(viewerToken).getStatus());
        UUID alexOffer=show(viewerToken,Set.of("age:27"));
        assertEquals(SELECTED,next(candidateToken).getStatus());
        UUID taylorOffer=show(candidateToken,Set.of("age:27"));
        UUID matchId=UUID.randomUUID();
        FeedbackEvent alexLike=FeedbackEvent.reaction(UUID.randomUUID(),alexOffer,2,FeedbackEvent.Type.LIKE);
        FeedbackEvent taylorLike=FeedbackEvent.reaction(UUID.randomUUID(),taylorOffer,2,FeedbackEvent.Type.LIKE);
        Instant acceptedAt=clock.instant();
        BackendMatchConfirmation alexMatch=new BackendMatchConfirmation(matchId,viewerId,candidateId,alexOffer,alexLike.id(),acceptedAt);
        BackendMatchConfirmation taylorMatch=new BackendMatchConfirmation(matchId,candidateId,viewerId,taylorOffer,taylorLike.id(),acceptedAt);
        ConfirmMatchInteractor matches=new ConfirmMatchInteractor(data);
        ReplayAcceptedLikeInteractor likes=new ReplayAcceptedLikeInteractor(data);
        assertEquals(FeedbackResult.PENDING,matches.execute(taylorMatch));
        long revision=data.getFeedRevision();
        assertEquals(FeedbackResult.REJECTED,matches.execute(new BackendMatchConfirmation(UUID.randomUUID(),viewerId,candidateId,alexOffer,alexLike.id(),acceptedAt)));
        assertEquals(FeedbackResult.REJECTED,matches.execute(new BackendMatchConfirmation(matchId,candidateId,viewerId,taylorOffer,UUID.randomUUID(),acceptedAt)));
        assertEquals(FeedbackResult.REJECTED,likes.execute(new BackendAcceptedLike(viewerId,candidateId,taylorLike,acceptedAt)));
        FeedbackEvent wrongExposure=FeedbackEvent.reaction(taylorLike.id(),alexOffer,2,FeedbackEvent.Type.LIKE);
        assertEquals(FeedbackResult.REJECTED,likes.execute(new BackendAcceptedLike(candidateId,viewerId,wrongExposure,acceptedAt)));
        FeedbackEvent wrongLike=FeedbackEvent.reaction(UUID.randomUUID(),taylorOffer,2,FeedbackEvent.Type.LIKE);
        assertEquals(FeedbackResult.REJECTED,likes.execute(new BackendAcceptedLike(candidateId,viewerId,wrongLike,acceptedAt)));
        assertEquals(InteractionOutputData.Status.REJECTED,send(candidateToken,wrongLike).getStatus());
        assertEquals(revision,data.getFeedRevision());
        assertEquals(FeedbackResult.PENDING,matches.execute(alexMatch));
        assertEquals(FeedbackResult.APPLIED,likes.execute(new BackendAcceptedLike(viewerId,candidateId,alexLike,acceptedAt)));
        assertEquals(FeedbackResult.APPLIED,likes.execute(new BackendAcceptedLike(candidateId,viewerId,taylorLike,acceptedAt)));
        assertEquals(FeedbackResult.DUPLICATE,matches.execute(alexMatch));
        assertEquals(FeedbackResult.DUPLICATE,matches.execute(taylorMatch));
        assertEquals(FeedbackResult.REJECTED,likes.execute(new BackendAcceptedLike(candidateId,viewerId,
                FeedbackEvent.reaction(UUID.randomUUID(),taylorOffer,3,FeedbackEvent.Type.LIKE),acceptedAt)));
        assertEquals(revision+2,data.getFeedRevision());
        assertEquals(1,data.engagementMetrics(viewerId).confirmations());
        assertEquals(1,data.engagementMetrics(candidateId).confirmations());
    }

    @Test void pendingConfirmationDoesNotAuthorizeNewUiLikeDuringBlockOrOutage() {
        ready(minimum("Taylor")); assertEquals(SELECTED,next(viewerToken).getStatus());
        UUID exposure=show(viewerToken,Set.of("age:27"));
        FeedbackEvent action=FeedbackEvent.reaction(UUID.randomUUID(),exposure,2,FeedbackEvent.Type.LIKE);
        BackendMatchConfirmation match=new BackendMatchConfirmation(UUID.randomUUID(),viewerId,candidateId,exposure,action.id(),clock.instant());
        assertEquals(FeedbackResult.PENDING,new ConfirmMatchInteractor(data).execute(match));
        data.setPairState(viewer,candidateId,new PairState(false,true,false,false,false,false,false));
        assertEquals(InteractionOutputData.Rejection.PREVIEW_UNAVAILABLE,send(viewerToken,action).getRejection().orElseThrow());
        data.setAvailable(false);
        assertEquals(InteractionOutputData.Status.RETRYABLE_FAILURE,send(viewerToken,action).getStatus());
        data.setAvailable(true);
        assertEquals(0,data.engagementMetrics(viewerId).likes());
        assertEquals(0,data.engagementMetrics(viewerId).confirmations());
    }

    @Test void detailAuthorizationAndActualRenderingAreSeparateAndRevealOnlyPublicEvidence() {
        ProfileFields.Input c=minimum("Taylor"); c.interestTags=List.of("ART","BOOKS","HIKING","MUSIC");
        c.smoking="NEVER"; c.drinking="SOMETIMES"; c.gender="WOMAN"; c.showGender=false;
        ready(c); assertEquals(SELECTED,next(viewerToken).getStatus());
        SelectedCandidateData preview=lastSelection();
        UUID exposure=UUID.randomUUID(); assertTrue(data.preparePreview(viewerToken,exposure,preview));
        assertEquals(0,data.engagementMetrics(viewerId).impressions());
        assertFalse(preview.getExposureKeys().contains("interest:MUSIC"));
        assertFalse(preview.getExposureKeys().contains("smoking:NEVER"));
        assertEquals(OpenProfileResult.Status.PROFILE_UNAVAILABLE,open(viewerToken,exposure).getStatus());
        FeedbackEvent forged=FeedbackEvent.previewShown(UUID.randomUUID(),exposure,1,Set.of("smoking:NEVER"));
        assertEquals(InteractionOutputData.Status.REJECTED,send(viewerToken,forged).getStatus());
        FeedbackEvent shown=FeedbackEvent.previewShown(UUID.randomUUID(),exposure,2,Set.of("interest:ART"));
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,shown).getStatus());
        assertEquals(InteractionOutputData.Status.ALREADY_APPLIED,send(viewerToken,shown).getStatus());
        FeedbackEvent rejectedDislike=FeedbackEvent.targetedDislike(UUID.randomUUID(),exposure,3,"smoking:NEVER");
        assertEquals(InteractionOutputData.Status.REJECTED,send(viewerToken,rejectedDislike).getStatus());
        assertEquals(OpenProfileResult.Status.PROFILE_UNAVAILABLE,open(candidateToken,exposure).getStatus());
        PublicProfileDetailData detail=open(viewerToken,exposure).getDetail().orElseThrow();
        assertTrue(detail.getPublicGender().isEmpty()); assertEquals(preview.getProfileRevision(),detail.getProfileRevision());
        assertTrue(detail.getExposureKeys().containsAll(Set.of("smoking:NEVER","interest:MUSIC")));
        assertEquals(0,data.engagementMetrics(viewerId).firstOpens());
        assertEquals(InteractionOutputData.Status.REJECTED,send(viewerToken,rejectedDislike).getStatus());
        FeedbackEvent opened=FeedbackEvent.profileOpened(UUID.randomUUID(),exposure,4,Set.of("smoking:NEVER"));
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,opened).getStatus());
        assertEquals(InteractionOutputData.Status.ALREADY_APPLIED,send(viewerToken,opened).getStatus());
        assertEquals(OpenProfileResult.Status.READY,open(viewerToken,exposure).getStatus());
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,
                FeedbackEvent.profileOpened(UUID.randomUUID(),exposure,5,Set.of("drinking:SOMETIMES"))).getStatus());
        assertEquals(1,data.engagementMetrics(viewerId).firstOpens());
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,
                FeedbackEvent.targetedDislike(UUID.randomUUID(),exposure,6,"smoking:NEVER")).getStatus());
        assertEquals(-0.9,affinity(data.load(viewer,date).getSnapshot().orElseThrow(),"smoking:NEVER"));
        assertEquals(0,affinity(data.load(viewer,date).getSnapshot().orElseThrow(),"interest:MUSIC"));
        assertEquals(EMPTY,next(newSession(UUID.randomUUID())).getStatus());
    }

    @Test void detailAndDelayedClientActionsRejectAChangedProfileRevision() {
        ready(minimum("Taylor")); assertEquals(SELECTED,next(viewerToken).getStatus());
        UUID exposure=show(viewerToken,Set.of("age:27"));
        success(data.commit(candidateToken,candidateId,3,SAVE,minimum("Changed")));
        assertEquals(OpenProfileResult.Status.PROFILE_UNAVAILABLE,open(viewerToken,exposure).getStatus());
        InteractionViewModel receipt=send(viewerToken,FeedbackEvent.reaction(UUID.randomUUID(),exposure,2,FeedbackEvent.Type.LIKE));
        assertEquals(InteractionOutputData.Rejection.PREVIEW_UNAVAILABLE,receipt.getRejection().orElseThrow());
        assertEquals(0,data.engagementMetrics(viewerId).likes());
        assertEquals(SELECTED,next(viewerToken).getStatus());
        UUID fresh=show(viewerToken,Set.of("age:27"));
        assertNotEquals(exposure,fresh);
        assertEquals(2,data.engagementMetrics(viewerId).impressions());
    }

    @Test void actualEngagementAndConfirmedOutcomesSurviveLearningResetAndDeduplicateReports() {
        ready(minimum("Taylor")); assertEquals(SELECTED,next(viewerToken).getStatus());
        UUID exposure=show(viewerToken,Set.of("age:27"));
        assertEquals(InteractionOutputData.Status.REJECTED,send(viewerToken,FeedbackEvent.activeView(UUID.randomUUID(),exposure,2,0)).getStatus());
        assertEquals(OpenProfileResult.Status.READY,open(viewerToken,exposure).getStatus());
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,FeedbackEvent.profileOpened(UUID.randomUUID(),exposure,3,Set.of("age:27"))).getStatus());
        clock.advance(Duration.ofSeconds(30));
        assertEquals(InteractionOutputData.Status.REJECTED,send(viewerToken,FeedbackEvent.activeView(UUID.randomUUID(),exposure,4,31_000)).getStatus());
        FeedbackEvent dwell=FeedbackEvent.activeView(UUID.randomUUID(),exposure,5,25_000);
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,dwell).getStatus());
        assertEquals(InteractionOutputData.Status.ALREADY_APPLIED,send(viewerToken,dwell).getStatus());
        UUID like=UUID.randomUUID();
        assertEquals(InteractionOutputData.Status.ACCEPTED,send(viewerToken,FeedbackEvent.reaction(like,exposure,6,FeedbackEvent.Type.LIKE,30_000)).getStatus());
        success(data.commit(viewerToken,viewerId,3,SAVE,minimum("Renamed")));
        assertEquals(0.45,affinity(data.load(viewer,date).getSnapshot().orElseThrow(),"age:27"),1e-12);
        ProfileFields.Input edited=minimum("Viewer"); edited.preferredInterestTags=List.of("ART");
        success(data.commit(viewerToken,viewerId,4,SAVE,edited));
        assertEquals(0,affinity(data.load(viewer,date).getSnapshot().orElseThrow(),"age:27"));
        assertEquals(FeedbackResult.APPLIED,new ConfirmMatchInteractor(data).execute(
                new BackendMatchConfirmation(UUID.randomUUID(),viewerId,candidateId,exposure,like,clock.instant())));
        assertEquals(0,affinity(data.load(viewer,date).getSnapshot().orElseThrow(),"age:27"));
        assertEquals(0,data.engagementMetrics(viewerId).matureImpressions());
        clock.advance(Duration.ofHours(24));
        EngagementMetrics metrics=data.engagementMetrics(viewerId);
        assertEquals(1,metrics.impressions()); assertEquals(1,metrics.firstOpens()); assertEquals(30_000,metrics.activeMillis());
        assertEquals(1,metrics.likes()); assertEquals(1,metrics.confirmations()); assertEquals(1,metrics.matureImpressions());
        assertEquals(1,metrics.matureMatches()); assertEquals(1,metrics.openRate()); assertEquals(1,metrics.matureMatchRate());
        assertTrue(data.load(viewer,date.plusDays(1)).getSnapshot().orElseThrow().getExplorationEvidence().isEmpty());
    }

    @Test void racingRetriesCommitOneReceiptOnePairChangeAndOneLearningContribution() throws Exception {
        ready(minimum("Taylor")); assertEquals(SELECTED,next(viewerToken).getStatus());
        UUID exposure=show(viewerToken,Set.of("age:27"));
        FeedbackEvent swipe=FeedbackEvent.reaction(UUID.randomUUID(),exposure,2,FeedbackEvent.Type.SWIPE);
        long revision=data.getFeedRevision();
        ExecutorService executor=Executors.newFixedThreadPool(2); CountDownLatch go=new CountDownLatch(1);
        try {
            Callable<FeedbackResult> delivery=()->{ go.await(); return data.applyFeedback(viewerToken,swipe); };
            Future<FeedbackResult> first=executor.submit(delivery), second=executor.submit(delivery); go.countDown();
            assertEquals(Set.of(FeedbackResult.APPLIED,FeedbackResult.DUPLICATE),
                    new HashSet<>(List.of(first.get(5,TimeUnit.SECONDS),second.get(5,TimeUnit.SECONDS))));
        } finally { executor.shutdownNow(); }
        assertEquals(revision+1,data.getFeedRevision());
        assertEquals(EMPTY,next(viewerToken).getStatus());
        assertEquals(-0.20,affinity(data.load(viewer,date).getSnapshot().orElseThrow(),"age:27"),1e-12);
    }

    @Test void seededExplorationNeverSelectsBlockedHardIneligibleOrOutOfBandCandidates() {
        ProfileFields.Input person=minimum("Alex"); person.preferredInterestTags=List.of("HIKING");
        person.minYears=25; person.maxYears=30; person.ageMode="SOFT";
        publish(viewerToken,viewerId,person);
        publish(candidateToken,candidateId,aged("Best",27)); data.setPairState(viewer,candidateId,PairState.clear());
        ProfileId near=new ProfileId(100000003); addCandidate(near,aged("Eligible nearby score",32));
        ProfileId low=new ProfileId(100000004); addCandidate(low,aged("Outside band",45));
        ProfileId blocked=new ProfileId(100000005); addCandidate(blocked,aged("Blocked",31));
        data.setPairState(viewer,blocked,new PairState(false,true,false,false,false,false,false));
        ProfileId hard=new ProfileId(100000006); ProfileFields.Input hardProfile=aged("Rejects viewer",32);
        hardProfile.minYears=35; hardProfile.maxYears=40; hardProfile.ageMode="HARD"; addCandidate(hard,hardProfile);
        assertEquals(candidateId,next(viewerToken).getCard().orElseThrow().getProfileId());
        ExplorationPolicy policy=new ExplorationPolicy(new Random(42),1,0.10,0.02,0.02,0.10,2);
        assertEquals(near,explore(viewerToken,policy).getProfileId());
        data.setPairState(viewer,near,new PairState(true,false,false,false,false,false,false));
        assertEquals(candidateId,explore(viewerToken,policy).getProfileId());
    }

    @Test void realMatureCohortsNarrowTheExplorationBandAndPreferenceEditResetsIt() {
        ProfileFields.Input person=minimum("Alex"); person.preferredInterestTags=List.of("HIKING");
        person.minYears=25; person.maxYears=30; person.ageMode="SOFT";
        publish(viewerToken,viewerId,person); publish(candidateToken,candidateId,aged("Best",27));
        data.setPairState(viewer,candidateId,PairState.clear());
        for (int i=0;i<4;i++) {
            UUID session=newSession(UUID.randomUUID()); assertEquals(SELECTED,next(session).getStatus());
            UUID id=show(session,Set.of());
            if (i>=2) {
                assertEquals(OpenProfileResult.Status.READY,open(session,id).getStatus());
                assertEquals(InteractionOutputData.Status.ACCEPTED,
                        send(session,FeedbackEvent.profileOpened(UUID.randomUUID(),id,2,Set.of())).getStatus());
            }
            assertEquals(InteractionOutputData.Status.ACCEPTED,
                    send(session,FeedbackEvent.reaction(UUID.randomUUID(),id,3,FeedbackEvent.Type.CLOSE)).getStatus());
            clock.advance(Duration.ofSeconds(1));
        }
        ProfileId alt=new ProfileId(100000003); addCandidate(alt,aged("Alternative",36));
        ExplorationPolicy policy=new ExplorationPolicy(new Random(22),1,0.20,0.05,0.10,0.10,2);
        assertEquals(alt,explore(viewerToken,policy).getProfileId());
        clock.advance(Duration.ofHours(24));
        assertEquals(candidateId,explore(viewerToken,policy).getProfileId());
        person.maxYears=31; success(data.commit(viewerToken,viewerId,3,SAVE,person));
        assertEquals(alt,explore(viewerToken,policy).getProfileId());
        assertEquals(4,data.engagementMetrics(viewerId).matureImpressions());
    }

    private ProfileFields.Input aged(String name,int age) {
        ProfileFields.Input input=minimum(name); input.dateOfBirth=date.minusYears(age); input.interestTags=List.of("HIKING"); return input;
    }

    private SelectedCandidateData explore(UUID token,ExplorationPolicy policy) {
        List<NextPreviewOutputData> results=new ArrayList<>();
        DiscoveryController engine=new DiscoveryController(new NextPreviewInteractor(data,data,results::add,
                new EligibilityPolicy(),new ViewerScorer(),clock,policy));
        UUID request=UUID.randomUUID(); engine.next(request,token);
        assertEquals(1,results.size()); assertEquals(request,results.get(0).getRequestId());
        return results.get(0).getSelectedCandidate().orElseThrow();
    }

    private OpenProfileOutputData open(UUID token, UUID exposure) {
        UUID request=UUID.randomUUID(); int count=details.size(); interactions.open(request,token,exposure);
        assertEquals(count+1,details.size()); OpenProfileOutputData detail=details.get(count);
        assertEquals(request,detail.getRequestId()); assertEquals(exposure,detail.getExposureId()); return detail;
    }

    private UUID newSession(UUID discoverySession) {
        UUID token=UUID.randomUUID(); data.addSession(token,new ResolvedViewer(viewerId,UUID.randomUUID(),discoverySession)); return token;
    }

    private static final class TestClock extends Clock {
        private final java.util.concurrent.atomic.AtomicReference<Instant> now;
        private final ZoneId zone;
        TestClock(Instant start, ZoneId zone) { this(new java.util.concurrent.atomic.AtomicReference<>(start),zone); }
        private TestClock(java.util.concurrent.atomic.AtomicReference<Instant> now, ZoneId zone) { this.now=now; this.zone=zone; }
        void advance(Duration elapsed) { now.updateAndGet(instant->instant.plus(elapsed)); }
        @Override public Instant instant() { return now.get(); }
        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId zone) { return new TestClock(now,zone); }
    }

    private void addCandidate(ProfileId id,ProfileFields.Input input) {
        UUID token=UUID.randomUUID(); data.addSession(token,new ResolvedViewer(id,UUID.randomUUID(),UUID.randomUUID()));
        publish(token,id,input); data.setPairState(viewer,id,PairState.clear());
    }

    private double affinity(DiscoverySnapshot snapshot,String key) {
        return snapshot.getViewerPreferences().affinityAt(DiscoveryFeatures.indexOf(key));
    }

    private SelectedCandidateData lastSelection() {
        return outputs.get(outputs.size()-1).getSelectedCandidate().orElseThrow();
    }

    private InteractionViewModel send(UUID token,FeedbackEvent event) {
        int count=receipts.size(); interactions.interact(token,event);
        assertEquals(count+1,receipts.size());
        InteractionViewModel receipt=receipts.get(count);
        assertEquals(event.id(),receipt.getEventId()); assertEquals(event.exposureId(),receipt.getExposureId());
        return receipt;
    }

    private UUID show(UUID token, Set<String> keys) {
        UUID exposure=UUID.randomUUID();
        assertTrue(data.preparePreview(token,exposure,lastSelection()));
        assertEquals(InteractionOutputData.Status.ACCEPTED,
                send(token,FeedbackEvent.previewShown(UUID.randomUUID(),exposure,1,keys)).getStatus());
        return exposure;
    }
    @Test void lifecycleRevisionOrderingNormalizationAndFailuresPreserveState() {
        DiscoveryProfile draft=success(data.createDraft(viewerToken)); assertEquals(1,draft.getProfileRevision());
        assertEquals(ProfileCommitResult.Status.INVALID_TRANSITION,data.createDraft(viewerToken).getStatus());
        long feed=data.getFeedRevision();
        ProfileCommitResult missing=data.commit(viewerToken,viewerId,1,PUBLISH,null);
        assertEquals(Set.of("displayName","dateOfBirth","audience"),missing.getFieldErrors().keySet());
        assertSame(draft,data.getOwnedProfile(viewerToken,viewerId).orElseThrow()); assertEquals(feed,data.getFeedRevision());
        assertEquals(ProfileCommitResult.Status.STALE_REVISION,data.commit(viewerToken,viewerId,0,RESUME,null).getStatus());
        assertEquals(ProfileCommitResult.Status.INVALID_TRANSITION,data.commit(viewerToken,viewerId,1,RESUME,null).getStatus());
        ProfileFields.Input fields=minimum(" Alex "); success(data.commit(viewerToken,viewerId,1,SAVE,fields));
        feed=data.getFeedRevision(); fields.displayName="Alex";
        assertEquals(2,success(data.commit(viewerToken,viewerId,2,SAVE,fields)).getProfileRevision()); assertEquals(feed,data.getFeedRevision());
        success(data.commit(viewerToken,viewerId,2,PUBLISH,null));
        DiscoveryProfile active=data.getOwnedProfile(viewerToken,viewerId).orElseThrow(); feed=data.getFeedRevision();
        ProfileFields.Input invalid=new ProfileFields.Input(); invalid.gender="WRONG";
        ProfileCommitResult failed=data.commit(viewerToken,viewerId,3,SAVE,invalid);
        assertEquals(ProfileCommitResult.Status.VALIDATION_FAILED,failed.getStatus());
        assertEquals(Set.of("gender","displayName","dateOfBirth","audience"),failed.getFieldErrors().keySet());
        assertSame(active,data.getOwnedProfile(viewerToken,viewerId).orElseThrow()); assertEquals(feed,data.getFeedRevision());
        assertEquals(ProfileCommitResult.Status.STALE_REVISION,data.commit(viewerToken,viewerId,2,SAVE,invalid).getStatus());
        assertEquals(ProfileCommitResult.Status.PROFILE_UNAVAILABLE,data.commit(candidateToken,viewerId,3,SAVE,fields).getStatus());
        assertEquals(ProfileCommitResult.Status.PROFILE_UNAVAILABLE,data.commit(viewerToken,new ProfileId(100000099),1,PAUSE,null).getStatus());
    }
    @Test void photoOwnershipAndIndependentValidationErrorsAreEnforced() {
        success(data.createDraft(viewerToken)); UUID owned=UUID.randomUUID(),foreign=UUID.randomUUID();
        data.registerPhoto(owned,viewerId); data.registerPhoto(foreign,candidateId);
        ProfileFields.Input in=minimum("Viewer"); in.photoIds=List.of(foreign);
        assertEquals(FieldError.NOT_OWNED,data.commit(viewerToken,viewerId,1,SAVE,in).getFieldErrors().get("photoIds"));
        in.photoIds=List.of(UUID.randomUUID()); in.gender="INVALID";
        assertEquals(Set.of("photoIds","gender"),data.commit(viewerToken,viewerId,1,SAVE,in).getFieldErrors().keySet());
        in.gender=null; in.photoIds=List.of(owned,owned);
        assertEquals(FieldError.INVALID_VALUE,data.commit(viewerToken,viewerId,1,SAVE,in).getFieldErrors().get("photoIds"));
        in.photoIds=List.of(owned); assertEquals(List.of(owned),success(data.commit(viewerToken,viewerId,1,SAVE,in)).getFields().getPhotoIds());
        assertThrows(IllegalArgumentException.class,()->data.registerPhoto(owned,candidateId));
        assertTrue(data.getOwnedProfile(candidateToken,viewerId).isEmpty());
    }
    @Test void competingSavesAtSameRevisionHaveExactlyOneWinner() throws Exception {
        ready(minimum("Taylor")); long feed=data.getFeedRevision();
        CountDownLatch start=new CountDownLatch(1); ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<ProfileCommitResult> a=pool.submit(()->{start.await();return data.commit(candidateToken,candidateId,3,SAVE,minimum("First"));});
            Future<ProfileCommitResult> b=pool.submit(()->{start.await();return data.commit(candidateToken,candidateId,3,SAVE,minimum("Second"));});
            start.countDown();
            List<ProfileCommitResult.Status> statuses=List.of(a.get(5,TimeUnit.SECONDS).getStatus(),b.get(5,TimeUnit.SECONDS).getStatus());
            assertEquals(1,Collections.frequency(statuses,ProfileCommitResult.Status.SUCCESS));
            assertEquals(1,Collections.frequency(statuses,ProfileCommitResult.Status.STALE_REVISION));
            DiscoveryProfile committed=data.getOwnedProfile(candidateToken,candidateId).orElseThrow();
            assertEquals(4,committed.getProfileRevision()); assertEquals(feed+1,data.getFeedRevision());
            DiscoveryViewModel.Card card=next(viewerToken).getCard().orElseThrow();
            assertEquals(committed.getFields().getDisplayName().orElseThrow(),card.getDisplayName()); assertEquals(4,card.getProfileRevision());
        } finally { pool.shutdownNow(); assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS)); }
    }
    @Test void failedCommitsAndSessionFailuresCannotMutatePublishedProfile() {
        ready(minimum("Taylor"));
        DiscoveryProfile original=data.getOwnedProfile(candidateToken,candidateId).orElseThrow();
        long feed=data.getFeedRevision();
        ProfileFields.Input underage=minimum("Taylor"); underage.dateOfBirth=date.minusYears(18).plusDays(1);
        ProfileCommitResult rejected=data.commit(candidateToken,candidateId,3,SAVE,underage);
        assertEquals(FieldError.OUT_OF_RANGE,rejected.getFieldErrors().get("dateOfBirth"));
        assertSame(original,data.getOwnedProfile(candidateToken,candidateId).orElseThrow());
        assertEquals(ProfileCommitResult.Status.INVALID_TRANSITION,data.commit(candidateToken,candidateId,3,PUBLISH,null).getStatus());
        assertEquals(ProfileCommitResult.Status.VALIDATION_FAILED,data.commit(candidateToken,candidateId,3,SAVE,null).getStatus());
        data.setAvailable(false);
        assertEquals(ProfileCommitResult.Status.RETRYABLE_FAILURE,data.commit(candidateToken,candidateId,3,PAUSE,null).getStatus());
        assertEquals(ProfileCommitResult.Status.RETRYABLE_FAILURE,data.createDraft(UUID.randomUUID()).getStatus());
        data.setAvailable(true);
        assertEquals(ProfileCommitResult.Status.SESSION_EXPIRED,data.commit(UUID.randomUUID(),candidateId,3,PAUSE,null).getStatus());
        assertEquals(ProfileCommitResult.Status.SESSION_EXPIRED,data.createDraft(UUID.randomUUID()).getStatus());
        assertSame(original,data.getOwnedProfile(candidateToken,candidateId).orElseThrow());
        assertEquals(feed,data.getFeedRevision()); assertEquals(SELECTED,next(viewerToken).getStatus());
    }
    @Test void fullSaveClearsOmittedFieldsAndOwnInterestsDoNotBecomePreferences() {
        ProfileFields.Input c=minimum("Taylor"); c.bio="Bio"; c.gender="NON_BINARY"; c.locationArea="VAUGHAN";
        c.relationshipIntent="OPEN_TO_EITHER"; c.smoking="SOMETIMES"; c.drinking="NEVER";
        c.interestTags=List.of("ART"); c.preferredInterestTags=List.of("BOOKS");
        ready(c); ProfileFields f=data.getOwnedProfile(candidateToken,candidateId).orElseThrow().getFields();
        assertEquals(Set.of(InterestTag.ART),f.getInterestTags().orElseThrow());
        assertEquals(Set.of(InterestTag.BOOKS),f.getPreferences().getPreferredInterestTags());
        success(data.commit(candidateToken,candidateId,3,SAVE,minimum("Taylor")));
        f=data.getOwnedProfile(candidateToken,candidateId).orElseThrow().getFields();
        assertTrue(f.getBio().isEmpty()); assertTrue(f.getGender().isEmpty()); assertTrue(f.getLocationArea().isEmpty());
        assertTrue(f.getRelationshipIntent().isEmpty()); assertTrue(f.getSmoking().isEmpty()); assertTrue(f.getDrinking().isEmpty());
        assertTrue(f.getInterestTags().isEmpty()); assertTrue(f.getPreferences().getPreferredInterestTags().isEmpty());
        assertEquals(SELECTED,next(viewerToken).getStatus());
    }
    @Test void activeSaveAndReadRaceNeverMixProfileFieldsAndRevisions() throws Exception {
        ready(minimum("Before")); CountDownLatch start=new CountDownLatch(1); ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<ProfileCommitResult> saved=pool.submit(()->{start.await();return data.commit(candidateToken,candidateId,3,SAVE,minimum("After"));});
            Future<DiscoverySnapshot> read=pool.submit(()->{start.await();return data.load(viewer,date).getSnapshot().orElseThrow();});
            start.countDown(); success(saved.get(5,TimeUnit.SECONDS));
            CandidateSnapshot candidate=read.get(5,TimeUnit.SECONDS).getCandidates().get(0);
            long revision=candidate.getProfile().getProfileRevision();
            assertEquals(revision,candidate.getPublicProfile().getProfileRevision());
            assertEquals(revision==3?"Before":"After",candidate.getPublicProfile().getDisplayName());
            assertTrue(revision==3||revision==4);
            assertEquals("After",next(viewerToken).getCard().orElseThrow().getDisplayName());
        } finally {pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }
    @ParameterizedTest @ValueSource(ints={0,1,4999,5000,5001,10000,10001})
    void presenterDistanceBucketsNeverRevealExactMetres(int metres) {
        String expected=metres==0?"Same area":"Within "+(5*(int)Math.ceil(metres/5000.0))+" km";
        assertEquals(expected,DiscoveryPresenter.distanceLabel(metres));
    }
    @Test void malformedControllerTextCannotInvokeUseCase() {
        int[] calls={0}; DiscoveryController input=new DiscoveryController(i->calls[0]++);
        assertThrows(IllegalArgumentException.class,()->input.next("bad",viewerToken.toString()));
        assertThrows(IllegalArgumentException.class,()->input.next("1-1-1-1-1",viewerToken.toString()));
        assertEquals(0,calls[0]); input.next(UUID.randomUUID().toString(),viewerToken.toString()); assertEquals(1,calls[0]);
    }
    @Test void publicBoundaryTypesAndSourceDependenciesRespectPrivacyAndCA() throws Exception {
        Set<String> forbidden=Set.of("dateOfBirth","gender","audience","preferences","preferredInterestTags","locationArea","latitude","longitude","password","email","phone","score","viewerPreferences","attributes","affinities");
        for(Class<?> type:List.of(PublicProfileSummary.class,SelectedCandidateData.class,PublicProfileDetailData.class,
                InteractionViewModel.class,DiscoveryViewModel.class,DiscoveryViewModel.Card.class)) {
            Set<String> fields=Arrays.stream(type.getDeclaredFields()).map(Field::getName).collect(Collectors.toSet());
            assertTrue(Collections.disjoint(forbidden,fields),type.getName());
            assertFalse(Arrays.stream(type.getDeclaredFields()).anyMatch(field->field.getType()==CandidateAttributeVector.class
                    || field.getType()==ViewerPreferenceVector.class),type.getName());
        }
        assertFalse(Arrays.stream(DiscoveryViewModel.Card.class.getDeclaredFields()).anyMatch(f->f.getName().equals("distanceMetres")));
        for(String path:List.of("src/main/java/entity/matchmaking","src/main/java/use_case/discovery")) {
            try(java.util.stream.Stream<Path> files=Files.walk(Paths.get(path))) {
                for(Path file:files.filter(f->f.toString().endsWith(".java")).collect(Collectors.toList())) {
                    String source=Files.readString(file);
                    assertFalse(source.matches("(?s).*import\\s+(javax\\.swing|data_access|interface_adapter)\\..*"),file.toString());
                }
            }
        }
    }
}
