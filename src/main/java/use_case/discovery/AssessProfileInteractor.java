package use_case.discovery;

import entity.matchmaking.ProfileAssessment;
import java.util.Objects;

/** Runs outside preview selection. A failed assessment never prevents a recommendation. */
public final class AssessProfileInteractor {
    private final ProfileAssessmentDataAccessInterface provider;

    public AssessProfileInteractor(ProfileAssessmentDataAccessInterface provider) { this.provider = Objects.requireNonNull(provider); }

    public AssessmentResult execute(AssessmentInput input) {
        AssessmentResult result;
        try {
            result = provider.assess(Objects.requireNonNull(input));
        } catch (DataAccessException failure) {
            return AssessmentResult.absent(AssessmentResult.Status.UNAVAILABLE);
        }
        if (result.status() != AssessmentResult.Status.READY) return result;
        ProfileAssessment assessment = result.assessment().orElseThrow();
        return assessment.schemaVersion() == AssessmentInput.SCHEMA_VERSION && assessment.rubricVersion() == AssessmentInput.RUBRIC_VERSION
                && assessment.profileRevision() == input.revision() ? result : AssessmentResult.absent(AssessmentResult.Status.INVALID_RESPONSE);
    }
}
