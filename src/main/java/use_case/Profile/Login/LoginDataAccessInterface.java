package use_case.Profile.Login;

import entity.Profile;

public interface LoginDataAccessInterface {
    Profile getProfile(String username);
}