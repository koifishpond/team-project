package data_access;

import entity.Preference;

import java.util.HashMap;
import java.util.Map;

public class PreferenceDatabase {
    private final Map<Integer, Preference> preferencesByUserId = new HashMap<>();

    public Preference getPreference(int userId) {
        return preferencesByUserId.get(userId);
    }

    public void savePreference(Preference preference) {
        if (preference == null) {
            return;
        }

        if (preference.getUserId() >= 0) {
            preferencesByUserId.put(preference.getUserId(), preference);
        }
    }

    public boolean existsByUserId(int userId) {
        return preferencesByUserId.containsKey(userId);
    }

    public void deletePreference(int userId) {
        preferencesByUserId.remove(userId);
    }
}
