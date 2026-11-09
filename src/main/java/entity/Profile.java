package entity;


public class Profile {
    private int userID; // between 100000000 and 999999999
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private Preference preferences;
    public static int userIDCounter = 100000000; //
    public Profile(int userID, String username, String password, String email, String phoneNumber)
            throws InvalidUserIDException {
        this.userID = userID;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        preferences = new Preference();
        if (userIDCounter < userID) {
            userIDCounter = userID;
        }
        else {
            throw new InvalidUserIDException();
        }
    }
    public Profile (String username, String password, String email, String phoneNumber) throws InvalidUserIDException {
        this(userIDCounter + 1, username, password, email, phoneNumber);
    }



    //TODO add getter and setters, add ways to reset/change pasword or user id/change email
}
