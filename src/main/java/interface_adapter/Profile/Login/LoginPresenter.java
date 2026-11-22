package interface_adapter.Profile.Login;

import use_case.Profile.Login.LoginOutputBoundary;
import use_case.Profile.Login.LoginOutputData;

public class LoginPresenter implements LoginOutputBoundary {
    private final LoginViewModel loginViewModel;

    public LoginPresenter(LoginViewModel loginViewModel) {
        this.loginViewModel = loginViewModel;
    }

    @Override
    public void prepareSuccessView(LoginOutputData outputData) {
        loginViewModel.setState(true, "Login successful.", outputData.getUsername());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        loginViewModel.setState(false, errorMessage, null);
    }
}