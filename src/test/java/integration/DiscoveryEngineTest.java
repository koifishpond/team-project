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
    private final Clock clock=Clock.fixed(Instant.parse("2026-11-17T02:00:00Z"),ZoneOffset.UTC);
    private final LocalDate date=LocalDate.of(2026,11,16);
    private final InMemoryDiscoveryDataAccess data=new InMemoryDiscoveryDataAccess(clock);
    private final ProfileId viewerId=new ProfileId(100000001), candidateId=new ProfileId(100000002);
    private final UUID viewerToken=UUID.randomUUID(), candidateToken=UUID.randomUUID();
    private final ResolvedViewer viewer=new ResolvedViewer(viewerId,UUID.randomUUID(),UUID.randomUUID());
    private final List<DiscoveryViewModel> models=new ArrayList<>();
    private final List<NextPreviewOutputData> outputs=new ArrayList<>();
    private final DiscoveryPresenter presenter=new DiscoveryPresenter(models::add);
    private final DiscoveryController controller=new DiscoveryController(new NextPreviewInteractor(data,data,
            output->{outputs.add(output);presenter.present(output);},new EligibilityPolicy(),new ViewerScorer(),clock));

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

    @Test void targetedLearningReordersTheSameEligiblePoolAndHardRulesStillWin() {
        ProfileFields.Input alex=minimum("Alex"); alex.locationArea="TORONTO";
        alex.preferredInterestTags=List.of("HIKING"); alex.maxKm=10; alex.distanceMode="SOFT";
        ProfileFields.Input taylor=minimum("Taylor"); taylor.locationArea="TORONTO"; taylor.interestTags=List.of("HIKING");
        ProfileFields.Input morgan=minimum("Morgan"); morgan.locationArea="MISSISSAUGA"; morgan.interestTags=List.of("HIKING");
        publish(viewerToken,viewerId,alex); publish(candidateToken,candidateId,taylor);
        ProfileId morganId=new ProfileId(100000003); addCandidate(morganId,morgan);
        data.setPairState(viewer,candidateId,PairState.clear());
        assertEquals(candidateId,next(viewerToken).getCard().orElseThrow().getProfileId());
        long before=data.getFeedRevision();
        UUID exposure=UUID.randomUUID();
        assertTrue(data.captureExposure(viewerToken,exposure,lastSelection(),Set.of("distance:0","interest:HIKING","age:27")));
        assertEquals(before,data.getFeedRevision());
        RecordFeedbackInteractor feedback=new RecordFeedbackInteractor(data);
        FeedbackEvent dislike=FeedbackEvent.targetedDislike(UUID.randomUUID(),exposure,"distance:0");
        assertEquals(FeedbackResult.REJECTED,feedback.execute(candidateToken,dislike));
        assertEquals(FeedbackResult.APPLIED,feedback.execute(viewerToken,dislike));
        assertEquals(FeedbackResult.DUPLICATE,feedback.execute(viewerToken,dislike));
        assertEquals(before+1,data.getFeedRevision());
        DiscoverySnapshot snapshot=data.load(viewer,date).getSnapshot().orElseThrow();
        assertEquals(-0.4,affinity(snapshot,"distance:0"),1e-12);
        assertEquals(0.5,affinity(snapshot,"interest:HIKING"));
        assertEquals(0,affinity(snapshot,"age:27"));
        assertTrue(snapshot.getCandidates().stream().noneMatch(candidate->candidate.getPairState().excludes()));
        assertEquals(morganId,next(viewerToken).getCard().orElseThrow().getProfileId());
        assertEquals(before+1,data.getFeedRevision());
        assertEquals(Set.of(InterestTag.HIKING),data.getOwnedProfile(viewerToken,viewerId).orElseThrow()
                .getFields().getPreferences().getPreferredInterestTags());
        alex.distanceMode="HARD";
        success(data.commit(viewerToken,viewerId,3,SAVE,alex));
        assertEquals(candidateId,next(viewerToken).getCard().orElseThrow().getProfileId());
        data.setPairState(viewer,candidateId,new PairState(false,true,false,false,false,false,false));
        assertEquals(EMPTY,next(viewerToken).getStatus());
    }

    @Test void acceptedLikeAndBackendMatchLearnFrozenEvidenceForOnlyTheOriginalViewer() {
        ProfileFields.Input taylor=minimum("Taylor"); taylor.interestTags=List.of("BOOKS");
        ready(taylor);
        ResolvedViewer reverse=data.resolve(candidateToken).getViewer().orElseThrow();
        data.setPairState(reverse,viewerId,PairState.clear());
        UUID exposure=UUID.randomUUID();
        assertEquals(SELECTED,next(viewerToken).getStatus());
        SelectedCandidateData shown=lastSelection();
        assertTrue(data.captureExposure(viewerToken,exposure,shown,Set.of("interest:BOOKS","smoking:NEVER")));
        assertFalse(data.captureExposure(viewerToken,exposure,shown,Set.of("age:27")));
        taylor.interestTags=List.of("ART"); taylor.smoking="NEVER";
        success(data.commit(candidateToken,candidateId,3,SAVE,taylor));
        assertFalse(data.captureExposure(viewerToken,UUID.randomUUID(),shown,Set.of("interest:BOOKS")));
        RecordFeedbackInteractor feedback=new RecordFeedbackInteractor(data);
        UUID likeId=UUID.randomUUID(), matchId=UUID.randomUUID();
        assertEquals(FeedbackResult.APPLIED,feedback.execute(viewerToken,FeedbackEvent.reaction(likeId,exposure,FeedbackEvent.Type.LIKE)));
        ViewerPreferenceVector afterLike=data.load(viewer,date).getSnapshot().orElseThrow().getViewerPreferences();
        assertEquals(0.45,afterLike.affinityAt(DiscoveryFeatures.indexOf("interest:BOOKS")),1e-12);
        FeedbackEvent match=FeedbackEvent.confirmedMatch(UUID.randomUUID(),exposure,likeId,matchId);
        assertEquals(FeedbackResult.REJECTED,feedback.execute(viewerToken,match));
        assertTrue(data.registerConfirmedMatch(matchId,viewerId,likeId));
        assertEquals(FeedbackResult.APPLIED,feedback.execute(viewerToken,match));
        assertEquals(FeedbackResult.DUPLICATE,feedback.execute(viewerToken,
                FeedbackEvent.confirmedMatch(UUID.randomUUID(),exposure,likeId,matchId)));
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
        assertFalse(data.captureExposure(viewerToken,exposure,shown,Set.of("age:27")));
        data.setPairState(viewer,candidateId,PairState.clear());
        assertEquals(SELECTED,next(viewerToken).getStatus());
        assertTrue(data.captureExposure(viewerToken,exposure,lastSelection(),Set.of("age:27")));
        RecordFeedbackInteractor feedback=new RecordFeedbackInteractor(data);
        FeedbackEvent opened=FeedbackEvent.reaction(UUID.randomUUID(),exposure,FeedbackEvent.Type.OPEN);
        data.setAvailable(false);
        assertEquals(FeedbackResult.RETRYABLE_FAILURE,feedback.execute(viewerToken,opened));
        data.setAvailable(true); data.expireSession(viewerToken);
        assertEquals(FeedbackResult.SESSION_EXPIRED,feedback.execute(viewerToken,opened));
        assertEquals(0,affinity(data.load(viewer,date).getSnapshot().orElseThrow(),"age:27"));
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
        for(Class<?> type:List.of(PublicProfileSummary.class,SelectedCandidateData.class,DiscoveryViewModel.class,DiscoveryViewModel.Card.class)) {
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
