package data_access;

import entity.Preference;

public interface PreferenceDataAccessInterface {
    Preference getPreference(int userId);

    void savePreference(Preference preference);

    boolean existsByUserId(int userId);

    void deletePreference(int userId);
}
