package use_case;

import entity.InvalidUserIDException;

public interface ProfileInputBoundary {
    void execute(ProfileInputData profileInputData) throws InvalidUserIDException;
}
