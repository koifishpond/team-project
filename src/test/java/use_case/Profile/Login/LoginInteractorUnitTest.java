package use_case.Profile.Login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

import entity.Profile;

class LoginInteractorUnitTest {

    @Test
    void successfulLoginReturnsTheMatchingProfileIdentity() {
        Profile profile = new Profile("alice", "secret", "alice@example.com", "4165551234");
        FakeLoginDataAccess dataAccess = new FakeLoginDataAccess(profile);
        CapturingLoginOutput output = new CapturingLoginOutput();
        LoginInteractor interactor = new LoginInteractor(dataAccess, output);

        interactor.execute(new LoginInputData("alice", "secret"));

        assertEquals("alice", output.successData.getUsername());
        assertEquals(profile.getUserId(), output.successData.getUserId());
        assertNull(output.failureMessage);
    }

    @Test
    void invalidCredentialsProduceAFailureWithoutAUserIdentity() {
        FakeLoginDataAccess dataAccess = new FakeLoginDataAccess(
                new Profile("alice", "secret", "alice@example.com", "4165551234"));
        CapturingLoginOutput output = new CapturingLoginOutput();
        LoginInteractor interactor = new LoginInteractor(dataAccess, output);

        interactor.execute(new LoginInputData("alice", "wrong"));

        assertNull(output.successData);
        assertEquals("Invalid username or password.", output.failureMessage);
    }

    private static final class FakeLoginDataAccess implements LoginDataAccessInterface {
        private final Profile profile;

        private FakeLoginDataAccess(Profile profile) {
            this.profile = profile;
        }

        @Override
        public Profile getProfile(String username) {
            return profile.getUsername().equals(username) ? profile : null;
        }
    }

    private static final class CapturingLoginOutput implements LoginOutputBoundary {
        private LoginOutputData successData;
        private String failureMessage;

        @Override
        public void prepareSuccessView(LoginOutputData outputData) {
            successData = outputData;
            failureMessage = null;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            successData = null;
            failureMessage = errorMessage;
        }
    }
}