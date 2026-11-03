package data;

import java.time.LocalDate;
import java.util.Set;

public class Preference {
    public String firstName;
    public String lastName;
    public Set<String> attractedTo; //contains only a limited selection of gender (for algorithm) currently Male/Female
    public LocalDate dateOfBirth;
    public int age;
    public String gender; // same as attractedTo
    public Preference () { //some users might not want to input data, so we default set to null
        firstName = null;
        lastName = null;
        attractedTo = null;
        dateOfBirth = null;
        age = -1;
        gender = null;
    }

    //TODO add getter setter methods for preferences (user can change preferences/edit profile)
}
