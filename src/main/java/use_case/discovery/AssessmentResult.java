package use_case.discovery;

import entity.matchmaking.ProfileAssessment;
import java.util.Objects;
import java.util.Optional;

public final class AssessmentResult {
    public enum Status { READY, INSUFFICIENT_EVIDENCE, NOT_CONFIGURED, TIMEOUT, UNAVAILABLE, INVALID_RESPONSE }
    private final Status status;
    private final ProfileAssessment assessment;

    private AssessmentResult(Status status, ProfileAssessment assessment) {
        this.status = Objects.requireNonNull(status);
        this.assessment = assessment;
    }

    public static AssessmentResult ready(ProfileAssessment value) {
        return new AssessmentResult(Status.READY, Objects.requireNonNull(value));
    }

    public static AssessmentResult absent(Status status) {
        if (status == Status.READY) throw new IllegalArgumentException("READY requires an assessment");
        return new AssessmentResult(status, null);
    }

    public Status status() { return status; }
    public Optional<ProfileAssessment> assessment() { return Optional.ofNullable(assessment); }
}
