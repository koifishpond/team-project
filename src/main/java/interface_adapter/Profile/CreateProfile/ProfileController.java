package interface_adapter.Profile.CreateProfile;

import use_case.Profile.CreateProfile.ProfileInputBoundary;
import use_case.Profile.CreateProfile.ProfileInputData;

public class ProfileController {
    private final ProfileInputBoundary profileInputBoundary;

    public ProfileController(ProfileInputBoundary profileInputBoundary) {
        this.profileInputBoundary = profileInputBoundary;
    }

    public void createProfile(String username, String password, String email, String phoneNumber) {
        profileInputBoundary.execute(new ProfileInputData(username, password, email, phoneNumber));
    }
}
