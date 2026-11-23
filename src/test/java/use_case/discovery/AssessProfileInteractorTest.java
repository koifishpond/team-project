package use_case.discovery;

import entity.matchmaking.ProfileAssessment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class AssessProfileInteractorTest {
    private final AssessmentInput input = new AssessmentInput(AssessmentInput.Fixture.SPARSE, 5);

    @Test void evenAnAlternateProviderCannotSupplyAStaleAssessment() {
        ProfileAssessment old = new ProfileAssessment(1, 1, 4, "fixture", 0.4, ProfileAssessment.Personality.UNKNOWN);
        AssessmentResult result = new AssessProfileInteractor(data -> AssessmentResult.ready(old)).execute(input);
        assertEquals(AssessmentResult.Status.INVALID_RESPONSE, result.status());
        assertTrue(result.assessment().isEmpty());
    }

    @Test void aPortFailureBecomesAnAbsentAssessment() {
        AssessmentResult result = new AssessProfileInteractor(data -> {
            throw new DataAccessException("fixture provider failure");
        }).execute(input);
        assertEquals(AssessmentResult.Status.UNAVAILABLE, result.status());
        assertTrue(result.assessment().isEmpty());
    }
}
