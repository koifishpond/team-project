package interface_adapter.Profile.Login;

import use_case.Profile.Login.LoginInputBoundary;
import use_case.Profile.Login.LoginInputData;

public class LoginController {
    private final LoginInputBoundary loginInputBoundary;

    public LoginController(LoginInputBoundary loginInputBoundary) {
        this.loginInputBoundary = loginInputBoundary;
    }

    public void login(String username, String password) {
        loginInputBoundary.execute(new LoginInputData(username, password));
    }
}