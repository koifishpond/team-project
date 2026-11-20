package use_case.discovery;

import java.util.*;

public final class NextPreviewOutputData {
    public enum Status { SELECTED, EMPTY, SESSION_EXPIRED, VIEWER_UNAVAILABLE, RETRYABLE_FAILURE }
    private final UUID requestId;
    private final Status status;
    private final SelectedCandidateData selectedCandidate;
    public NextPreviewOutputData(UUID requestId, Status status, Optional<SelectedCandidateData> selected) {
        this.requestId = Objects.requireNonNull(requestId); this.status = Objects.requireNonNull(status);
        if ((status == Status.SELECTED) != selected.isPresent()) throw new IllegalArgumentException("Result payload");
        selectedCandidate = selected.orElse(null);
    }
    public UUID getRequestId() { return requestId; }
    public Status getStatus() { return status; }
    public Optional<SelectedCandidateData> getSelectedCandidate() { return Optional.ofNullable(selectedCandidate); }
}
