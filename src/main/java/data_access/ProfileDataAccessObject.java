package data_access;

import entity.Profile;

public class ProfileDataAccessObject implements ProfileDataAccessInterface {
    private final Database database;

    public ProfileDataAccessObject(Database database) {
        this.database = database;
    }

    @Override
    public Profile getProfile(String username) {
        // TODO: delegate to the database layer.
        return database.getProfile(username);
    }

    @Override
    public void saveProfile(Profile profile) {
        // TODO: delegate to the database layer.
        database.saveProfile(profile);
    }

    @Override
    public boolean existsByUsername(String username) {
        // TODO: check existence through the database.
        return database.existsByUsername(username);
    }

    @Override
    public void deleteProfile(String username) {
        // TODO: remove through the database.
        database.deleteProfile(username);
    }
}
