package interface_adapter.Profile;

import use_case.Profile.ProfileInputBoundary;
import use_case.Profile.ProfileInputData;

public class ProfileController {
    private final ProfileInputBoundary profileInputBoundary;

    public ProfileController(ProfileInputBoundary profileInputBoundary) {
        this.profileInputBoundary = profileInputBoundary;
    }

    public void createProfile(String username, String password, String email, String phoneNumber) {
        // TODO: create the input data object and send it to the use case.
        ProfileInputData inputData = new ProfileInputData(username, password, email, phoneNumber);
        profileInputBoundary.execute(inputData);
    }
}
