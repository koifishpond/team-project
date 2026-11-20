package use_case.discovery;

import java.util.Objects;
import java.util.Optional;

public final class OpenProfileResult {
    public enum Status { READY, PROFILE_UNAVAILABLE, SESSION_EXPIRED, RETRYABLE_FAILURE }
    private final Status status;
    private final PublicProfileDetailData detail;

    private OpenProfileResult(Status status, PublicProfileDetailData detail) {
        this.status = Objects.requireNonNull(status);
        this.detail = detail;
    }

    public static OpenProfileResult unavailable(Status status) {
        if (status == Status.READY) throw new IllegalArgumentException("READY needs detail");
        return new OpenProfileResult(status, null);
    }

    public static OpenProfileResult ready(PublicProfileDetailData detail) {
        return new OpenProfileResult(Status.READY, Objects.requireNonNull(detail));
    }

    public Status getStatus() { return status; }
    public Optional<PublicProfileDetailData> getDetail() { return Optional.ofNullable(detail); }
}
