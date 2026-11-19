package entity;

public class Profile {
    private final String username;
    private String password;
    private String email;
    private String phoneNumber;
    private static int nextUserId = 100000000;
    private final int userId;

    public Profile(String username, String password, String email, String phoneNumber) {
        this(username, password, email, phoneNumber, nextGeneratedUserId(), true);
    }

    public Profile(String username, String password, String email, String phoneNumber, int userId) {
        this(username, password, email, phoneNumber, userId, false);
    }

    private Profile(String username, String password, String email, String phoneNumber,
                    int userId, boolean generatedId) {
        if (userId < 100000000 || userId > 999999999) {
            throw new InvalidUserIDException();
        }
        if (!generatedId) {
            reserveUserId(userId);
        }

        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.userId = userId;
    }

    private static int nextGeneratedUserId() {
        if (nextUserId > 999999999) {
            throw new InvalidUserIDException();
        }
        return nextUserId++;
    }

    private static void reserveUserId(int userId) {
        if (userId < nextUserId) {
            throw new InvalidUserIDException();
        }
        nextUserId = userId + 1;
    }

    //Bypasses the NextUserID detection as profile was exported from a unorder hasmap and there was no effiecient way to get the next user id. This method is used for restoring profiles from a database.
    public static Profile restore(String username, String password, String email,
                                               String phoneNumber, int userId) { 
        Profile profile = new Profile(username, password, email, phoneNumber, userId, true);
        nextUserId = Math.max(nextUserId, userId + 1);
        return profile;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getUserId() {
        return userId;
    }

    public void setNewEmail(String newEmail) {
        this.email = newEmail;
    }

    public void setNewPassword(String newPassword) {
        this.password = newPassword;
    }

    public void setNewPhoneNumber(String newPhoneNumber) {
        this.phoneNumber = newPhoneNumber;
    }
}
