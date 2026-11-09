package entity;

import java.time.LocalDate;
import java.util.Set;

public class Preference {
    public String firstName;
    public String lastName;
    public Set<String> attractedTo; //contains only a limited selection of gender (for algorithm) currently Male/Female
    public LocalDate dateOfBirth;
    public int age;
    public String gender; // same as attractedTo
    public String intent; // intent == 'Short' or intent == 'Long' or intent == 'NA'
    public float[] location; // contains Only 2 values representing longitude and latitude to 3 decimal places
    public static final String[] LISTOFALLINTEREST = {"Interest"}; // TODO add possible interests that the user
                                                                    // can choose from (So we have a fixed set of
                                                                    // interest for algorithm)
    public String[] interests; // maybe use a hash set here depends on who is doing it
    public String description;
    public Preference () { //some users might not want to input data, so we default set to null
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

    //TODO add getter setter methods for preferences (user can change preferences/edit profile)

    //TODO add method that allows users to select the interest and record them in Interest
}