package use_case.discovery;

import entity.matchmaking.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Public-only output: exact metres are for Presenter formatting, never the View Model. */
public final class SelectedCandidateData {
    private final ProfileId profileId;
    private final long profileRevision, feedRevision;
    private final LocalDate effectiveDate;
    private final String displayName;
    private final int ageYears;
    private final Optional<UUID> primaryPhotoId;
    private final OptionalInt distanceMetres;
    private final Optional<String> bioExcerpt;
    private final List<InterestTag> previewInterestTags;
    public SelectedCandidateData(PublicProfileSummary source, long feedRevision, LocalDate date, OptionalInt metres) {
        if (feedRevision < 1 || (metres.isPresent() && metres.getAsInt() < 0)) throw new IllegalArgumentException("Revision or distance");
        profileId = source.getProfileId(); profileRevision = source.getProfileRevision();
        this.feedRevision = feedRevision; effectiveDate = Objects.requireNonNull(date);
        displayName = source.getDisplayName(); ageYears = source.getAgeYears(); primaryPhotoId = source.getPrimaryPhotoId();
        distanceMetres = Objects.requireNonNull(metres);
        bioExcerpt = source.getBio().map(SelectedCandidateData::excerpt);
        previewInterestTags = List.copyOf(source.getInterestTags().orElse(Set.of()).stream().sorted().limit(3).collect(Collectors.toList()));
    }
    private static String excerpt(String bio) {
        String flat = bio.replaceAll("(?U)\\s+", " ").strip();
        return flat.codePointCount(0, flat.length()) <= 120 ? flat : flat.substring(0, flat.offsetByCodePoints(0, 119)) + "…";
    }
    public ProfileId getProfileId() { return profileId; }
    public long getProfileRevision() { return profileRevision; }
    public long getFeedRevision() { return feedRevision; }
    public LocalDate getEffectiveDate() { return effectiveDate; }
    public String getDisplayName() { return displayName; }
    public int getAgeYears() { return ageYears; }
    public Optional<UUID> getPrimaryPhotoId() { return primaryPhotoId; }
    public OptionalInt getDistanceMetres() { return distanceMetres; }
    public Optional<String> getBioExcerpt() { return bioExcerpt; }
    public List<InterestTag> getPreviewInterestTags() { return previewInterestTags; }
    public Set<String> getExposureKeys() { return DiscoveryFeatures.publicKeys(ageYears, distanceMetres, previewInterestTags); }
}
