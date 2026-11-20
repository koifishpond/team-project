package interface_adapter.Login;

import use_case.Login.LoginOutputBoundary;
import use_case.Login.LoginOutputData;

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