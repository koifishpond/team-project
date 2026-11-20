package use_case.discovery;

import entity.matchmaking.CandidateAttributeVector;
import entity.matchmaking.ProfileId;
import java.time.LocalDate;
import java.util.*;

/** Immutable, revision-bound preview offer. A render acknowledgement freezes a subset of its keys. */
public final class FeedbackExposure {
    private final UUID id;
    private final ProfileId candidate;
    private final long viewerRevision;
    private final long candidateRevision;
    private final LocalDate date;
    private final Set<String> previewKeys;

    public FeedbackExposure(UUID id, ProfileId candidate, long viewerRevision, long candidateRevision,
                            LocalDate date, Set<String> previewKeys) {
        this.id = Objects.requireNonNull(id);
        this.candidate = Objects.requireNonNull(candidate);
        if (viewerRevision < 1 || candidateRevision < 1) throw new IllegalArgumentException("Invalid revision");
        this.viewerRevision = viewerRevision;
        this.candidateRevision = candidateRevision;
        this.date = Objects.requireNonNull(date);
        this.previewKeys = Set.copyOf(previewKeys);
    }

    public UUID id() { return id; }
    public ProfileId candidate() { return candidate; }
    public long viewerRevision() { return viewerRevision; }
    public long candidateRevision() { return candidateRevision; }
    public LocalDate date() { return date; }
    public Set<String> previewKeys() { return previewKeys; }

    public static Set<String> presentKeys(CandidateAttributeVector attributes, Set<String> offered) {
        DiscoveryFeatures.requireCurrent(attributes.version(), attributes.dimension());
        Set<String> known = new TreeSet<>();
        for (String key : offered) {
            int index = DiscoveryFeatures.indexOf(key);
            if (attributes.isKnown(index) && attributes.valueAt(index) > 0) known.add(key);
        }
        return Collections.unmodifiableSet(known);
    }
}
