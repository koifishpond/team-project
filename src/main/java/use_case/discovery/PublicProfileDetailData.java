package use_case.discovery;

import entity.matchmaking.*;
import java.util.*;

/** Authorized public detail. Private profile fields and learned state have no place in this payload. */
public final class PublicProfileDetailData {
    private final UUID exposureId;
    private final ProfileId profileId;
    private final long profileRevision;
    private final String displayName;
    private final int ageYears;
    private final List<UUID> photoIds;
    private final Optional<String> bio;
    private final Optional<Gender> publicGender;
    private final Optional<String> areaLabel;
    private final Optional<RelationshipIntent> relationshipIntent;
    private final Optional<Frequency> smoking;
    private final Optional<Frequency> drinking;
    private final OptionalInt distanceMetres;
    private final List<InterestTag> interestTags;
    private final Set<String> exposureKeys;

    public PublicProfileDetailData(UUID exposureId, MatchingProfile profile, ProfileFields publicSource,
                                   OptionalInt distanceMetres, Set<String> exposureKeys) {
        this.exposureId = Objects.requireNonNull(exposureId);
        profileId = profile.getProfileId();
        profileRevision = profile.getProfileRevision();
        displayName = publicSource.getDisplayName().orElseThrow();
        ageYears = profile.getAgeYears().orElseThrow();
        photoIds = List.copyOf(publicSource.getPhotoIds());
        bio = publicSource.getBio();
        publicGender = publicSource.isShowGender() ? publicSource.getGender() : Optional.empty();
        areaLabel = publicSource.getLocationArea().map(LocationArea::getLabel);
        relationshipIntent = publicSource.getRelationshipIntent();
        smoking = publicSource.getSmoking();
        drinking = publicSource.getDrinking();
        this.distanceMetres = Objects.requireNonNull(distanceMetres);
        List<InterestTag> ordered = new ArrayList<>(publicSource.getInterestTags().orElse(Set.of()));
        Collections.sort(ordered);
        interestTags = List.copyOf(ordered);
        this.exposureKeys = Set.copyOf(exposureKeys);
    }

    public UUID getExposureId() { return exposureId; }
    public ProfileId getProfileId() { return profileId; }
    public long getProfileRevision() { return profileRevision; }
    public String getDisplayName() { return displayName; }
    public int getAgeYears() { return ageYears; }
    public List<UUID> getPhotoIds() { return photoIds; }
    public Optional<String> getBio() { return bio; }
    public Optional<Gender> getPublicGender() { return publicGender; }
    public Optional<String> getAreaLabel() { return areaLabel; }
    public Optional<RelationshipIntent> getRelationshipIntent() { return relationshipIntent; }
    public Optional<Frequency> getSmoking() { return smoking; }
    public Optional<Frequency> getDrinking() { return drinking; }
    public OptionalInt getDistanceMetres() { return distanceMetres; }
    public List<InterestTag> getInterestTags() { return interestTags; }
    public Set<String> getExposureKeys() { return exposureKeys; }
}
