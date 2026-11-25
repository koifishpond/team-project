package interface_adapter.Profile.ResetPassword;

import use_case.Profile.ResetPassword.ResetPasswordOutputBoundary;

public class ResetPasswordPresenter implements ResetPasswordOutputBoundary {
    private final ResetPasswordViewModel viewModel;

    public ResetPasswordPresenter(ResetPasswordViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareNewPasswordView() {
        viewModel.showNewPasswordStep();
    }

    @Override
    public void prepareSuccessView() {
        viewModel.complete("Password reset successfully. Please log in.");
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showMessage(errorMessage);
    }
}