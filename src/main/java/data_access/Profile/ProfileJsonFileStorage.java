package data_access.Profile;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.ObjectMapper;
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
}