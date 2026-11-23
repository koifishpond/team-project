package interface_adapter.Profile.Login;

import use_case.Profile.Login.LoginOutputBoundary;
import use_case.Profile.Login.LoginOutputData;

public class LoginPresenter implements LoginOutputBoundary {
    private final LoginViewModel loginViewModel;
    private final LoginToken loginToken;

    public LoginPresenter(LoginViewModel loginViewModel, LoginToken loginToken) {
        this.loginViewModel = loginViewModel;
        this.loginToken = loginToken;
    }

    @Override
    public void prepareSuccessView(LoginOutputData outputData) {
        loginToken.setUserId(outputData.getUserId());
        loginViewModel.setState(true, "Login successful.", outputData.getUsername());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        loginToken.clear();
        loginViewModel.setState(false, errorMessage, null);
    }
}