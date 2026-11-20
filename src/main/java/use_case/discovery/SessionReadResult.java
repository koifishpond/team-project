package use_case.discovery;

import entity.matchmaking.*;
import java.time.*;
import java.util.*;
public final class SessionReadResult {
    public enum Status { RESOLVED, SESSION_EXPIRED, UNAVAILABLE }
    private final Status status;
    private final ResolvedViewer viewer;
    private SessionReadResult(Status status, ResolvedViewer viewer) { this.status = status; this.viewer = viewer; }
    public static SessionReadResult success(ResolvedViewer viewer) { return new SessionReadResult(Status.RESOLVED, Objects.requireNonNull(viewer)); }
    public static SessionReadResult failure(Status status) {
        if (Objects.requireNonNull(status) == Status.RESOLVED) throw new IllegalArgumentException("Success requires data");
        return new SessionReadResult(status, null);
    }
    public Status getStatus() { return status; }
    public Optional<ResolvedViewer> getViewer() { return Optional.ofNullable(viewer); }
}
