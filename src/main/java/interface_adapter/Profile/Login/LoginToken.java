package interface_adapter.Profile.Login;

public final class LoginToken {
    private Integer userId;

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void clear() {
        userId = null;
    }
}