package use_case.discovery;

import java.util.UUID;

public final class InteractionOutputData {
    public enum Status { ACCEPTED, ALREADY_APPLIED, REJECTED, SESSION_EXPIRED, RETRYABLE_FAILURE }
    public enum Rejection { INVALID_EVENT, PREVIEW_UNAVAILABLE }
    private final UUID eventId;
    private final UUID exposureId;
    private final Status status;
    private final Rejection rejection;

    InteractionOutputData(FeedbackEvent event, FeedbackResult result) {
        eventId = event.id();
        exposureId = event.exposureId();
        rejection = result == FeedbackResult.REJECTED ? Rejection.INVALID_EVENT
                : result == FeedbackResult.PREVIEW_UNAVAILABLE ? Rejection.PREVIEW_UNAVAILABLE : null;
        switch (result) {
            case APPLIED: status = Status.ACCEPTED; break;
            case DUPLICATE: status = Status.ALREADY_APPLIED; break;
            case REJECTED: case PREVIEW_UNAVAILABLE: status = Status.REJECTED; break;
            case SESSION_EXPIRED: status = Status.SESSION_EXPIRED; break;
            case RETRYABLE_FAILURE: status = Status.RETRYABLE_FAILURE; break;
            default: throw new IllegalArgumentException("Backend delivery status in client interaction");
        }
    }

    public UUID getEventId() { return eventId; }
    public UUID getExposureId() { return exposureId; }
    public Status getStatus() { return status; }
    public java.util.Optional<Rejection> getRejection() { return java.util.Optional.ofNullable(rejection); }
}
