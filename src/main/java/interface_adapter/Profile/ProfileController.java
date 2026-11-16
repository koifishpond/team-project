package interface_adapter.Profile;

import use_case.Profile.ProfileInputBoundary;
import use_case.Profile.ProfileInputData;

public class ProfileController {
    private final ProfileInputBoundary profileInputBoundary;

    public ProfileController(ProfileInputBoundary profileInputBoundary) {
        this.profileInputBoundary = profileInputBoundary;
    }

    public void createProfile(String username, String password, String email, String phoneNumber) {
        profileInputBoundary.execute(new ProfileInputData(username, password, email, phoneNumber));
    }
}
