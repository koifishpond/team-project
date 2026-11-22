package use_case.Profile.Login;

import entity.Profile;

import java.util.Objects;

public class LoginInteractor implements LoginInputBoundary {
    private final LoginDataAccessInterface loginDataAccess;
    private final LoginOutputBoundary loginOutputBoundary;

    public LoginInteractor(LoginDataAccessInterface loginDataAccess,
                           LoginOutputBoundary loginOutputBoundary) {
        this.loginDataAccess = loginDataAccess;
        this.loginOutputBoundary = loginOutputBoundary;
    }

    @Override
    public void execute(LoginInputData inputData) {
        String username = inputData.getUsername();
        String password = inputData.getPassword();
        if (username == null || username.trim().isEmpty() || password == null) {
            loginOutputBoundary.prepareFailView("Invalid username or password.");
            return;
        }

        Profile profile = loginDataAccess.getProfile(username);
        if (profile == null || !Objects.equals(profile.getPassword(), password)) {
            loginOutputBoundary.prepareFailView("Invalid username or password.");
            return;
        }

        loginOutputBoundary.prepareSuccessView(new LoginOutputData(profile.getUsername()));
    }
}