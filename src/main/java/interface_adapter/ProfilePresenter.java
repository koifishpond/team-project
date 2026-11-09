package interface_adapter;

import use_case.ProfileOutputBoundary;
import use_case.ProfileOutputData;

public class ProfilePresenter implements ProfileOutputBoundary {
    @Override
    public void prepareSuccessView(ProfileOutputData profileOutputData) {
        // TODO: format the successful result for the view.
    }

    @Override
    public void prepareFailView(String errorMessage) {
        // TODO: format the failure result for the view.
    }
}
