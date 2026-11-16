package interface_adapter.Profile;

import use_case.Profile.ProfileOutputBoundary;
import use_case.Profile.ProfileOutputData;

public class ProfilePresenter implements ProfileOutputBoundary {
    private ProfileOutputData outputData;

    @Override
    public void prepareSuccessView(ProfileOutputData profileOutputData) {
        this.outputData = profileOutputData;
    }

    @Override
    public void prepareFailView(String errorMessage) {
        this.outputData = new ProfileOutputData(false, errorMessage, null);
    }

    public ProfileOutputData getOutputData() {
        return outputData;
    }
}
