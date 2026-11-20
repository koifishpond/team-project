package use_case.discovery;

import entity.matchmaking.*;
import java.util.*;

/** Local fixture profile commit result. Live Profile integration remains owned by its team. */
public final class ProfileCommitResult {
    public enum Status { SUCCESS, VALIDATION_FAILED, STALE_REVISION, INVALID_TRANSITION,
        PROFILE_UNAVAILABLE, SESSION_EXPIRED, RETRYABLE_FAILURE }
    private final Status status;
    private final DiscoveryProfile profile;
    private final Map<String, FieldError> fieldErrors;
    private ProfileCommitResult(Status status, DiscoveryProfile profile, Map<String, FieldError> errors) {
        this.status = Objects.requireNonNull(status); this.profile = profile; fieldErrors = Map.copyOf(errors);
    }
    public static ProfileCommitResult success(DiscoveryProfile profile) {
        return new ProfileCommitResult(Status.SUCCESS, Objects.requireNonNull(profile), Map.of());
    }
    public static ProfileCommitResult failure(Status status) {
        if (status == Status.SUCCESS || status == Status.VALIDATION_FAILED) throw new IllegalArgumentException("Payload required");
        return new ProfileCommitResult(status, null, Map.of());
    }
    public static ProfileCommitResult invalid(Map<String, FieldError> errors) {
        if (errors.isEmpty()) throw new IllegalArgumentException("Errors required");
        return new ProfileCommitResult(Status.VALIDATION_FAILED, null, errors);
    }
    public Status getStatus() { return status; }
    public Optional<DiscoveryProfile> getProfile() { return Optional.ofNullable(profile); }
    public Map<String, FieldError> getFieldErrors() { return fieldErrors; }
}
