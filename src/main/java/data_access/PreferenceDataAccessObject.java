package data_access;

import entity.Preference;

public class PreferenceDataAccessObject implements PreferenceDataAccessInterface {
    private final PreferenceDatabase preferenceDatabase;

    public PreferenceDataAccessObject(PreferenceDatabase preferenceDatabase) {
        this.preferenceDatabase = preferenceDatabase;
    }

    @Override
    public Preference getPreference(int userId) {
        return preferenceDatabase.getPreference(userId);
    }

    @Override
    public void savePreference(Preference preference) {
        preferenceDatabase.savePreference(preference);
    }

    @Override
    public boolean existsByUserId(int userId) {
        return preferenceDatabase.existsByUserId(userId);
    }

    @Override
    public void deletePreference(int userId) {
        preferenceDatabase.deletePreference(userId);
    }
}
