package use_case.discovery;

import entity.matchmaking.CandidateAttributeVector;
import entity.matchmaking.ProfileId;
import java.util.*;

/** Known, present features actually shown by the trusted fixture, frozen at capture. */
public final class FeedbackExposure {
    private final UUID id;
    private final ProfileId candidate;
    private final int version;
    private final Set<String> evidence;

    public FeedbackExposure(UUID id, ProfileId candidate, CandidateAttributeVector attributes, Set<String> shown) {
        this.id = Objects.requireNonNull(id);
        this.candidate = Objects.requireNonNull(candidate);
        version = attributes.version();
        DiscoveryFeatures.requireCurrent(version, attributes.dimension());
        Set<String> present = new TreeSet<>();
        for (String feature : shown) {
            int index = DiscoveryFeatures.indexOf(feature);
            if (attributes.isKnown(index) && attributes.valueAt(index) > 0) present.add(feature);
        }
        evidence = Collections.unmodifiableSet(present);
    }

    public UUID id() { return id; }
    public ProfileId candidate() { return candidate; }
    public int version() { return version; }
    public Set<String> evidence() { return evidence; }
}
