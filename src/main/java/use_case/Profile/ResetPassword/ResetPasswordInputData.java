package use_case.Profile.ResetPassword;

public class ResetPasswordInputData {
    private final String username;
    private final String email;
    private final String newPassword;

    public ResetPasswordInputData(String username, String email, String newPassword) {
        this.username = username;
        this.email = email;
        this.newPassword = newPassword;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getNewPassword() {
        return newPassword;
    }
}