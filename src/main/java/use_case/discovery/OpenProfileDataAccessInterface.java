package use_case.discovery;

import java.util.UUID;

public interface OpenProfileDataAccessInterface {
    OpenProfileResult authorizeDetail(UUID sessionToken, UUID exposureId);
}
