package interface_adapter;

import use_case.ProfileInputBoundary;
import use_case.ProfileInputData;

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
