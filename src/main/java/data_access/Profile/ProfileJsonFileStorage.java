package data_access.Profile;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import entity.Profile;
import use_case.Profile.ProfileDataAccessInterface;

public class ProfileJsonFileStorage {
    private final ProfileDataAccessInterface profileDataAccess;
    private final ObjectMapper objectMapper;

    public ProfileJsonFileStorage(ProfileDataAccessInterface profileDataAccess) {
        this.profileDataAccess = profileDataAccess;
        this.objectMapper = new ObjectMapper();
    }

    public void saveAllProfilesToJson(Path destination) throws IOException {
        Path absoluteDestination = destination.toAbsolutePath();
        Path parentDirectory = absoluteDestination.getParent();
        Files.createDirectories(parentDirectory);

        Path temporaryFile = Files.createTempFile(parentDirectory, "profiles-", ".json.tmp");
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(temporaryFile.toFile(), profileDataAccess.getAllProfiles());
            try {
                Files.move(temporaryFile, absoluteDestination,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, absoluteDestination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    public void loadProfilesFromJson(Path source) throws IOException {
        if (Files.notExists(source)) {
            return;
        }

        JsonNode profileArray = objectMapper.readTree(source.toFile());
        if (profileArray == null || !profileArray.isArray()) {
            throw new IOException("Profile save file must contain a JSON array.");
        }

        List<Profile> profiles = new ArrayList<>();
        Set<String> usernames = new HashSet<>();
        Set<Integer> userIds = new HashSet<>();
        for (JsonNode profileNode : profileArray) {
            Profile profile = readProfile(profileNode);
            if (!usernames.add(profile.getUsername()) || !userIds.add(profile.getUserId())) {
                throw new IOException("Profile save file contains duplicate usernames or user IDs.");
            }
            if (profileDataAccess.existsByUsername(profile.getUsername())
                    || profileDataAccess.getProfileByUserId(profile.getUserId()) != null) {
                throw new IOException("Profile database already contains a saved profile identity.");
            }
            profiles.add(profile);
        }

        for (Profile profile : profiles) {
            profileDataAccess.saveProfile(profile);
        }
    }

    private Profile readProfile(JsonNode profileNode) throws IOException {
        if (!profileNode.isObject()) {
            throw new IOException("Each saved profile must be a JSON object.");
        }

        JsonNode usernameNode = profileNode.get("username");
        JsonNode userIdNode = profileNode.get("userId");
        if (usernameNode == null || !usernameNode.isTextual() || usernameNode.asText().trim().isEmpty()) {
            throw new IOException("Each saved profile must have a non-empty username.");
        }
        if (userIdNode == null || !userIdNode.canConvertToInt()) {
            throw new IOException("Each saved profile must have a valid user ID.");
        }

        int userId = userIdNode.intValue();
        if (userId < 100000000 || userId > 999999999) {
            throw new IOException("Saved profile user IDs must be between 100000000 and 999999999.");
        }

        return Profile.restore(
                usernameNode.asText(),
                readNullableText(profileNode, "password"),
                readNullableText(profileNode, "email"),
                readNullableText(profileNode, "phoneNumber"),
                userId
        );
    }

    private String readNullableText(JsonNode profileNode, String propertyName) throws IOException {
        JsonNode property = profileNode.get(propertyName);
        if (property == null || property.isNull()) {
            return null;
        }
        if (!property.isTextual()) {
            throw new IOException("Saved profile property '" + propertyName + "' must be text or null.");
        }
        return property.asText();
    }
}