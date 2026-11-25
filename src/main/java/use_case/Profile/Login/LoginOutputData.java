package use_case.Profile.Login;

public class LoginOutputData {
    private final String username;
    private final int userId;

    public LoginOutputData(String username, int userId) {
        this.username = username;
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public int getUserId() {
        return userId;
    }
}