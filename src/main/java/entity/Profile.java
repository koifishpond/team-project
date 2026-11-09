package entity;


public class Profile {
    private String username;
    private String password;
    private String email;
    private String phoneNumber;
    private Preference preferences;
    public Profile(String username, String password, String email, String phoneNumber) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
        preferences = new Preference();
    }

    //TODO add getter and setters, add ways to reset/change pasword or user id/change email
}
