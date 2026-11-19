package use_case.discovery;

import entity.matchmaking.*;
import java.util.Objects;

public final class CandidateSnapshot {
    private final MatchingProfile profile;
    private final PublicProfileSummary publicProfile;
    private final PairState pairState;
    public CandidateSnapshot(MatchingProfile profile, PublicProfileSummary publicProfile, PairState pairState) {
        this.profile = Objects.requireNonNull(profile);
        this.publicProfile = Objects.requireNonNull(publicProfile);
        this.pairState = Objects.requireNonNull(pairState);
        if (!profile.getProfileId().equals(publicProfile.getProfileId())
                || profile.getProfileRevision() != publicProfile.getProfileRevision()
                || profile.getAgeYears().orElse(-1) != publicProfile.getAgeYears()
                || !profile.getInterestTags().equals(publicProfile.getInterestTags()))
            throw new IllegalArgumentException("Inconsistent projections");
    }
    public MatchingProfile getProfile() { return profile; }
    public PublicProfileSummary getPublicProfile() { return publicProfile; }
    public PairState getPairState() { return pairState; }
}
