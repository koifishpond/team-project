package entity;

public class Profile {
    private final String username;
    private final String password;
    private final String email;
    private final String phoneNumber;
    private static int userIdCounter = 100000000; // User ID starts from 100000000 to 999999999 to avoid conflicts with other IDs
    private int userId;

    public Profile(String username, String password, String email, String phoneNumber) {
        this(username, password, email, phoneNumber, userIdCounter);
        userIdCounter++;
    }

    public Profile(String username, String password, String email, String phoneNumber, int userId) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        if (userId < 100000000 || userId > 999999999){
            throw new InvalidUserIDException();
        }
        if (userIdCounter < userId){
            throw new InvalidUserIDException();
        }
        this.userId = userId;
    }
    public Profile (String username, String password, String email, String phoneNumber) {
        this(userIDCounter + 1, username, password, email, phoneNumber);
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

    public void setUserId(int userId) {
        this.userId = userId;
    }

    //TODO add ways to reset/change password or user id/change email
}
