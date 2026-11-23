package use_case.discovery;

import java.util.Optional;
import java.util.UUID;

public final class OpenProfileOutputData {
    private final UUID requestId;
    private final UUID exposureId;
    private final OpenProfileResult result;

    OpenProfileOutputData(OpenProfileInputData request, OpenProfileResult result) {
        requestId = request.getRequestId();
        exposureId = request.getExposureId();
        this.result = java.util.Objects.requireNonNull(result);
    }

    public UUID getRequestId() { return requestId; }
    public UUID getExposureId() { return exposureId; }
    public OpenProfileResult.Status getStatus() { return result.getStatus(); }
    public Optional<PublicProfileDetailData> getDetail() { return result.getDetail(); }
}
