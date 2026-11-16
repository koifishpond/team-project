package use_case.Profile;

import entity.InvalidUserIDException;

public interface ProfileInputBoundary {
    void execute(ProfileInputData profileInputData) throws InvalidUserIDException;
}
