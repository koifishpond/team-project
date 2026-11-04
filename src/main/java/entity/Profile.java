package entity;


public class Profile {
    private String username;
    private String password;
    private String email;
    private Preference preferences;
    public Profile(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        preferences = new Preference();
    }

    //TODO add getter and setters, add ways to reset/change pasword or user id/change email
}
