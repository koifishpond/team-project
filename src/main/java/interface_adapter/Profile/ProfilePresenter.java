package interface_adapter.Profile;

import use_case.Profile.ProfileOutputBoundary;
import use_case.Profile.ProfileOutputData;

public class ProfilePresenter implements ProfileOutputBoundary {
    private final ProfileViewModel profileViewModel;

    public ProfilePresenter(ProfileViewModel profileViewModel) {
        this.profileViewModel = profileViewModel;
    }

    @Override
    public void prepareSuccessView(ProfileOutputData profileOutputData) {
        profileViewModel.setState(true, profileOutputData.getMessage(), profileOutputData.getUsername());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        profileViewModel.setState(false, errorMessage, null);
    }
}
