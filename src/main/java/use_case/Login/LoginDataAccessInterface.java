package use_case.Login;

import entity.Profile;

public interface LoginDataAccessInterface {
    Profile getProfileByUsername(String username);
}