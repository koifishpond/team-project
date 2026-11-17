package entity;

import java.time.LocalDate;
import java.util.Set;

public class Preference {
    public String firstName;
    public String lastName;
    public Set<String> attractedTo; // Limited gender selection for matching (currently Male/Female).
    public LocalDate dateOfBirth;
    public int age;
    public String gender; // Same category as attractedTo.
    public String intent; // "Short", "Long", or "NA".
    public float[] location; // Longitude and latitude, to three decimal places.
    public static final String[] LISTOFALLINTEREST = {"Interest"}; // TODO: Add the supported matching interests.
    public String[] interests;
    public String description;
    private int userId;

    public Preference() {
        this(-1);
    }

    public Preference(int userId) {
        this.userId = userId;
        firstName = null;
        lastName = null;
        attractedTo = null;
        dateOfBirth = null;
        age = -1;
        gender = null;
        intent = "NA";
        location = new float[2];
        description = null;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    // TODO: Add preference accessors and validation.
    // TODO: Add support for selecting and recording interests.
}