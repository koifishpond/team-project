package use_case.discovery;

import java.util.Objects;

public final class OpenProfileInteractor implements OpenProfileInputBoundary {
    private final OpenProfileDataAccessInterface profiles;
    private final OpenProfileOutputBoundary output;

    public OpenProfileInteractor(OpenProfileDataAccessInterface profiles, OpenProfileOutputBoundary output) {
        this.profiles = Objects.requireNonNull(profiles);
        this.output = Objects.requireNonNull(output);
    }

    @Override public void execute(OpenProfileInputData input) {
        OpenProfileResult result;
        try {
            result = profiles.authorizeDetail(input.getSessionToken(), input.getExposureId());
        } catch (DataAccessException failure) {
            result = OpenProfileResult.unavailable(OpenProfileResult.Status.RETRYABLE_FAILURE);
        }
        output.present(new OpenProfileOutputData(input, result));
    }
}
