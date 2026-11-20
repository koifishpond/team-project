package interface_adapter.discovery;

import use_case.discovery.InteractionOutputData;
import java.util.Optional;
import java.util.UUID;

/** Public receipt and a message suitable for the headless view. */
public final class InteractionViewModel {
    private final UUID eventId;
    private final UUID exposureId;
    private final InteractionOutputData.Status status;
    private final Optional<InteractionOutputData.Rejection> rejection;
    private final String message;

    InteractionViewModel(InteractionOutputData output, String message) {
        eventId = output.getEventId();
        exposureId = output.getExposureId();
        status = output.getStatus();
        rejection = output.getRejection();
        this.message = message;
    }

    public UUID getEventId() { return eventId; }
    public UUID getExposureId() { return exposureId; }
    public InteractionOutputData.Status getStatus() { return status; }
    public Optional<InteractionOutputData.Rejection> getRejection() { return rejection; }
    public String getMessage() { return message; }
}
