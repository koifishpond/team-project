package use_case.discovery;

import entity.matchmaking.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DiscoveryPolicyTest {
    private final EligibilityPolicy policy = new EligibilityPolicy();
    private final ViewerScorer scorer = new ViewerScorer();
    private final LocalDate date = LocalDate.of(2026, 11, 16);
    private ProfileFields.Input fields(int age) {
        ProfileFields.Input in = new ProfileFields.Input(); in.displayName = "Public name";
        in.dateOfBirth = date.minusYears(age); in.audience = "ANY"; return in;
    }
    private MatchingProfile profile(int id, ProfileFields.Input input) {
        return new DiscoveryProfile(new ProfileId(id), 1, DiscoveryStatus.ACTIVE, ProfileFields.from(input, date)).matchingOn(date);
    }
    private MatchingProfile plain(int id) { return profile(id, fields(27)); }

    @ParameterizedTest @ValueSource(ints = {0,1,2,3,4,5,6})
    void everyPairFlagExcludesEvenPerfectScoringCandidate(int flag) {
        boolean[] f = new boolean[7]; f[flag] = true;
        PairState pair = new PairState(f[0],f[1],f[2],f[3],f[4],f[5],f[6]);
        ProfileFields.Input viewer = fields(27); viewer.preferredInterestTags = List.of("ART"); viewer.minYears=18; viewer.maxYears=40; viewer.ageMode="SOFT";
        viewer.maxKm=10; viewer.distanceMode="SOFT";
        ProfileFields.Input candidate = fields(27); candidate.interestTags=List.of("ART");
        MatchingProfile v = profile(100000001,viewer), c = profile(100000002,candidate);
        assertEquals(1, scorer.score(v,c,OptionalInt.of(0)),1e-12);
        assertFalse(policy.eligible(v,c,pair,OptionalInt.of(0)));
    }
    @Test void selfInactiveAndUnpublishedProfilesExcludedInBothDirections() {
        MatchingProfile viewer = plain(100000001), candidate = plain(100000002);
        assertFalse(policy.eligible(viewer,viewer,PairState.clear()));
        for (DiscoveryStatus status : List.of(DiscoveryStatus.DRAFT, DiscoveryStatus.PAUSED)) {
            MatchingProfile inactive = new DiscoveryProfile(new ProfileId(100000002),1,status,ProfileFields.from(fields(27),date)).matchingOn(date);
            assertFalse(policy.eligible(viewer,inactive,PairState.clear()));
            assertFalse(policy.eligible(inactive,viewer,PairState.clear()));
        }
        ProfileFields.Input incomplete = fields(27); incomplete.displayName=null;
        MatchingProfile unpublished=profile(100000002,incomplete);
        assertFalse(policy.eligible(viewer,unpublished,PairState.clear()));
        assertFalse(policy.eligible(unpublished,viewer,PairState.clear()));
        assertTrue(policy.eligible(viewer,candidate,PairState.clear()));
    }
    @ParameterizedTest @ValueSource(booleans = {false,true})
    void hardAgeIsMutualInclusiveAndSoftDoesNotExclude(boolean reverse) {
        ProfileFields.Input owner=fields(27), other=fields(37); owner.minYears=25; owner.maxYears=35; owner.ageMode="HARD";
        assertPair(false,reverse,owner,other,OptionalInt.empty());
        other.dateOfBirth=date.minusYears(25); assertPair(true,reverse,owner,other,OptionalInt.empty());
        other.dateOfBirth=date.minusYears(35); assertPair(true,reverse,owner,other,OptionalInt.empty());
        other.dateOfBirth=date.minusYears(24); assertPair(false,reverse,owner,other,OptionalInt.empty());
        owner.ageMode="SOFT"; assertPair(true,reverse,owner,other,OptionalInt.empty());
    }
    @ParameterizedTest @ValueSource(booleans = {false,true})
    void audienceRulesAreMutualAndUnknownGenderOnlyPassesAny(boolean reverse) {
        ProfileFields.Input owner=fields(27), other=fields(27); owner.audience="SELECTED"; owner.acceptedGenders=List.of("WOMAN");
        assertPair(false,reverse,owner,other,OptionalInt.empty());
        other.gender="MAN"; assertPair(false,reverse,owner,other,OptionalInt.empty());
        other.gender="WOMAN"; assertPair(true,reverse,owner,other,OptionalInt.empty());
        owner.audience="ANY"; owner.acceptedGenders=null; other.gender=null; assertPair(true,reverse,owner,other,OptionalInt.empty());
    }
    @ParameterizedTest @ValueSource(booleans = {false,true})
    void distanceBoundariesAndUnknownAreMutual(boolean reverse) {
        ProfileFields.Input owner=fields(27), other=fields(27); owner.maxKm=10; owner.distanceMode="HARD";
        assertPair(true,reverse,owner,other,OptionalInt.of(10000));
        assertPair(false,reverse,owner,other,OptionalInt.of(10001));
        assertPair(false,reverse,owner,other,OptionalInt.empty());
        owner.distanceMode="SOFT"; assertPair(true,reverse,owner,other,OptionalInt.empty());
    }
    private void assertPair(boolean expected, boolean reverse, ProfileFields.Input a, ProfileFields.Input b, OptionalInt metres) {
        MatchingProfile x=profile(100000001,a), y=profile(100000002,b);
        assertEquals(expected, reverse ? policy.eligible(y,x,PairState.clear(),metres) : policy.eligible(x,y,PairState.clear(),metres));
    }
    @Test void workedScoreAndHigherInterestFit() {
        ProfileFields.Input v=fields(27); v.preferredInterestTags=List.of("HIKING","BOOKS");
        v.maxKm=20; v.distanceMode="SOFT"; v.minYears=25; v.maxYears=35; v.ageMode="SOFT";
        ProfileFields.Input c=fields(37); c.interestTags=List.of("HIKING");
        double first=scorer.score(profile(100000001,v),profile(100000002,c),OptionalInt.of(5000));
        assertEquals(0.6075,first,1e-12);
        c.interestTags=List.of("HIKING","BOOKS");
        assertEquals(0.9075,scorer.score(profile(100000001,v),profile(100000002,c),OptionalInt.of(5000)),1e-12);
    }
    @Test void viewerPreferencesReverseRankingAndCandidateSoftPreferencesDoNotMatter() {
        ProfileFields.Input v=fields(27), a=fields(27), b=fields(27);
        a.interestTags=List.of("ART"); b.interestTags=List.of("BOOKS");
        v.preferredInterestTags=List.of("ART");
        assertTrue(score(v,a)>score(v,b));
        v.preferredInterestTags=List.of("BOOKS"); assertTrue(score(v,a)<score(v,b));
        double before=score(v,a); a.preferredInterestTags=List.of("SPORTS"); a.minYears=100; a.maxYears=120; a.ageMode="SOFT";
        a.maxKm=1; a.distanceMode="SOFT"; assertEquals(before,score(v,a));
    }
    private double score(ProfileFields.Input v, ProfileFields.Input c) { return scorer.score(profile(100000001,v),profile(100000002,c),OptionalInt.empty()); }
    @Test void missingAndEmptyDataRetainAllWeights() {
        ProfileFields.Input v=fields(27), c=fields(27);
        assertEquals(0.5,score(v,c),1e-12);
        v.preferredInterestTags=List.of("HIKING"); assertEquals(0.5,score(v,c),1e-12);
        c.interestTags=List.of(); assertEquals(0.2,score(v,c),1e-12);
        c.interestTags=List.of("HIKING"); assertEquals(0.8,score(v,c),1e-12);
        v.preferredInterestTags=List.of(); assertEquals(0.5,score(v,c),1e-12);
        v.maxKm=1; v.distanceMode="SOFT"; v.minYears=18; v.maxYears=20; v.ageMode="SOFT";
        assertEquals(0.5,scorer.score(profile(100000001,v), new MatchingProfile(new ProfileId(100000002),1,
                DiscoveryStatus.DRAFT,false,OptionalInt.empty(),Optional.empty(),Optional.empty(),Optional.empty(),
                new PartnerPreferences(AudienceMode.UNSET,Set.of(),Set.of(),null,null)),OptionalInt.empty()),1e-12);
    }
    @Test void distanceUsesRoundedHaversineAndCatalogCoordinates() {
        assertEquals(0,GeoDistance.between(LocationArea.TORONTO,LocationArea.TORONTO));
        assertEquals(21967,GeoDistance.between(LocationArea.TORONTO,LocationArea.MISSISSAUGA));
        for (LocationArea a : LocationArea.values()) for (LocationArea b : LocationArea.values())
            assertEquals(GeoDistance.between(a,b),GeoDistance.between(b,a));
        ProfileFields.Input a=fields(27),b=fields(27); a.locationArea="TORONTO"; b.locationArea="MISSISSAUGA";
        assertEquals(OptionalInt.of(21967),policy.distance(profile(100000001,a),profile(100000002,b)));
        b.locationArea=null; assertTrue(policy.distance(profile(100000001,a),profile(100000002,b)).isEmpty());
    }
    @ParameterizedTest @ValueSource(doubles={Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,181,-181})
    void invalidCoordinatesRejected(double coordinate) { assertThrows(IllegalArgumentException.class,()->GeoDistance.metres(0,coordinate,0,0)); }
    @Test void latitudeAndNegativeDistanceAreRejected() {
        assertThrows(IllegalArgumentException.class,()->GeoDistance.metres(91,0,0,0));
        assertThrows(IllegalArgumentException.class,()->scorer.score(plain(100000001),plain(100000002),OptionalInt.of(-1)));
        assertThrows(IllegalArgumentException.class,()->policy.eligible(plain(100000001),plain(100000002),PairState.clear(),OptionalInt.of(-1)));
    }
}
