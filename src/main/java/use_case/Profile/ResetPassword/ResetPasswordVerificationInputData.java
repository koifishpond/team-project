package use_case.Profile.ResetPassword;

public class ResetPasswordVerificationInputData {
    private final String username;
    private final String email;

    public ResetPasswordVerificationInputData(String username, String email) {
        this.username = username;
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}