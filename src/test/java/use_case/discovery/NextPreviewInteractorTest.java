package use_case.discovery;

import entity.matchmaking.*;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static use_case.discovery.NextPreviewOutputData.Status.*;

class NextPreviewInteractorTest {
    private final LocalDate date=LocalDate.of(2026,11,16);
    private final Clock clock=Clock.fixed(Instant.parse("2026-11-17T02:00:00Z"),ZoneOffset.UTC);
    private final ResolvedViewer viewer=new ResolvedViewer(new ProfileId(100000001),UUID.randomUUID(),UUID.randomUUID());
    private DiscoveryProfile profile(int id) {
        ProfileFields.Input in=new ProfileFields.Input(); in.displayName="Public"; in.dateOfBirth=date.minusYears(27); in.audience="ANY";
        return new DiscoveryProfile(new ProfileId(id),1,DiscoveryStatus.ACTIVE,ProfileFields.from(in,date));
    }
    private CandidateSnapshot candidate(int id) { DiscoveryProfile p=profile(id); return new CandidateSnapshot(p.matchingOn(date),p.publicOn(date),PairState.clear()); }
    private DiscoverySnapshot snapshot(List<CandidateSnapshot> candidates) { return new DiscoverySnapshot(1,1,date,profile(100000001).matchingOn(date),candidates); }
    private NextPreviewOutputData invoke(ViewerSessionDataAccessInterface sessions,DiscoveryDataAccessInterface data) {
        List<NextPreviewOutputData> outputs=new ArrayList<>(); UUID request=UUID.randomUUID();
        new NextPreviewInteractor(sessions,data,outputs::add,new EligibilityPolicy(),new ViewerScorer(),clock).execute(new NextPreviewInputData(request,UUID.randomUUID()));
        assertEquals(1,outputs.size()); assertEquals(request,outputs.get(0).getRequestId()); return outputs.get(0);
    }
    @Test void deterministicNumericTieAcrossShufflesAndRepeatedReads() {
        List<CandidateSnapshot> candidates=new ArrayList<>(List.of(candidate(100000009),candidate(100000002),candidate(100000010)));
        Random random=new Random(207);
        for(int i=0;i<30;i++) {
            Collections.shuffle(candidates,random);
            NextPreviewOutputData result=invoke(t->SessionReadResult.success(viewer),(v,d)->DiscoveryReadResult.success(snapshot(candidates)));
            assertEquals(SELECTED,result.getStatus()); assertEquals(new ProfileId(100000002),result.getSelectedCandidate().orElseThrow().getProfileId());
            assertEquals(date,result.getSelectedCandidate().orElseThrow().getEffectiveDate());
        }
    }
    @Test void expiredAndUnavailableSessionsNeverReadCandidateData() {
        DiscoveryDataAccessInterface unread=(v,d)->{ fail("Candidates must not be read"); return null; };
        assertEquals(SESSION_EXPIRED,invoke(t->SessionReadResult.failure(SessionReadResult.Status.SESSION_EXPIRED),unread).getStatus());
        assertEquals(RETRYABLE_FAILURE,invoke(t->SessionReadResult.failure(SessionReadResult.Status.UNAVAILABLE),unread).getStatus());
    }
    @Test void emptyMissingViewerAndReadFailuresAreDistinctAndCorrelated() {
        ViewerSessionDataAccessInterface sessions=t->SessionReadResult.success(viewer);
        assertEquals(EMPTY,invoke(sessions,(v,d)->DiscoveryReadResult.success(snapshot(List.of()))).getStatus());
        assertEquals(VIEWER_UNAVAILABLE,invoke(sessions,(v,d)->DiscoveryReadResult.failure(DiscoveryReadResult.Status.VIEWER_UNAVAILABLE)).getStatus());
        assertEquals(RETRYABLE_FAILURE,invoke(sessions,(v,d)->DiscoveryReadResult.failure(DiscoveryReadResult.Status.UNAVAILABLE)).getStatus());
        assertEquals(RETRYABLE_FAILURE,invoke(sessions,(v,d)->{throw new DataAccessException("offline");}).getStatus());
        assertEquals(RETRYABLE_FAILURE,invoke(t->{throw new DataAccessException("session offline");},(v,d)->{fail();return null;}).getStatus());
    }
    @Test void snapshotDateAndAuthenticatedIdentityMustMatchRequest() {
        ViewerSessionDataAccessInterface sessions=t->SessionReadResult.success(viewer);
        assertEquals(RETRYABLE_FAILURE,invoke(sessions,(v,d)->DiscoveryReadResult.success(new DiscoverySnapshot(1,1,date.plusDays(1),profile(100000001).matchingOn(date),List.of()))).getStatus());
        assertEquals(RETRYABLE_FAILURE,invoke(sessions,(v,d)->DiscoveryReadResult.success(new DiscoverySnapshot(1,1,date,profile(100000002).matchingOn(date),List.of()))).getStatus());
        assertEquals(VIEWER_UNAVAILABLE,invoke(sessions,(v,d)->DiscoveryReadResult.success(new DiscoverySnapshot(1,1,date,
                new DiscoveryProfile(viewer.getViewerId(),1,DiscoveryStatus.PAUSED,profile(100000001).getFields()).matchingOn(date),List.of()))).getStatus());
    }
    @Test void malformedStoredStructureBecomesUnavailableRead() {
        CandidateSnapshot c=candidate(100000002);
        assertEquals(DiscoveryReadResult.Status.UNAVAILABLE,DiscoveryReadResult.validated(()->snapshot(List.of(c,c))).getStatus());
        assertEquals(DiscoveryReadResult.Status.UNAVAILABLE,DiscoveryReadResult.validated(()->snapshot(List.of(new CandidateSnapshot(c.getProfile(),c.getPublicProfile(),null)))).getStatus());
        assertEquals(DiscoveryReadResult.Status.UNAVAILABLE,DiscoveryReadResult.validated(()->snapshot(List.of(new CandidateSnapshot(c.getProfile(),profile(100000003).publicOn(date),PairState.clear())))).getStatus());
        assertEquals(DiscoveryReadResult.Status.UNAVAILABLE,DiscoveryReadResult.validated(()->new DiscoverySnapshot(0,1,date,profile(100000001).matchingOn(date),List.of())).getStatus());
        assertEquals(DiscoveryReadResult.Status.UNAVAILABLE,DiscoveryReadResult.validated(()->new DiscoverySnapshot(1,2,date,profile(100000001).matchingOn(date),List.of())).getStatus());
        List<CandidateSnapshot> source=new ArrayList<>(List.of(c)); DiscoverySnapshot frozen=snapshot(source); source.clear();
        assertEquals(1,frozen.getCandidates().size()); assertThrows(UnsupportedOperationException.class,()->frozen.getCandidates().clear());
    }
    @Test void presenterFailureAndProgrammingErrorsAreNeverRetriedOrMislabelled() {
        AtomicInteger calls=new AtomicInteger();
        NextPreviewInteractor interactor=new NextPreviewInteractor(t->SessionReadResult.success(viewer),(v,d)->DiscoveryReadResult.success(snapshot(List.of())),
                o->{calls.incrementAndGet();throw new DataAccessException("Presenter failure");},new EligibilityPolicy(),new ViewerScorer(),clock);
        assertThrows(DataAccessException.class,()->interactor.execute(new NextPreviewInputData(UUID.randomUUID(),UUID.randomUUID()))); assertEquals(1,calls.get());
        NextPreviewInteractor broken=new NextPreviewInteractor(t->{throw new IllegalStateException("Bug");},(v,d)->{fail();return null;},o->fail(),new EligibilityPolicy(),new ViewerScorer(),clock);
        assertThrows(IllegalStateException.class,()->broken.execute(new NextPreviewInputData(UUID.randomUUID(),UUID.randomUUID())));
    }
    @Test void invalidInputAndOutputCombinationsRejected() {
        assertThrows(NullPointerException.class,()->new NextPreviewInputData(null,UUID.randomUUID()));
        assertThrows(NullPointerException.class,()->new NextPreviewInputData(UUID.randomUUID(),null));
        assertThrows(IllegalArgumentException.class,()->new NextPreviewOutputData(UUID.randomUUID(),SELECTED,Optional.empty()));
        SelectedCandidateData selected=new SelectedCandidateData(profile(100000002).publicOn(date),1,date,OptionalInt.empty());
        assertThrows(IllegalArgumentException.class,()->new NextPreviewOutputData(UUID.randomUUID(),EMPTY,Optional.of(selected)));
    }
}
