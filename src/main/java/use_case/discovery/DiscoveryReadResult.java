package use_case.discovery;

import entity.matchmaking.*;
import java.time.*;
import java.util.*;
public final class DiscoveryReadResult {
    /** Data Access adapters validate raw stored structure at this boundary, before returning a read. */
    public static DiscoveryReadResult validated(java.util.function.Supplier<DiscoverySnapshot> decode) {
        try { return success(decode.get()); }
        catch (IllegalArgumentException | NullPointerException malformedStoredData) {
            return failure(Status.UNAVAILABLE);
        }
    }
    public enum Status { LOADED, VIEWER_UNAVAILABLE, UNAVAILABLE }
    private final Status status;
    private final DiscoverySnapshot snapshot;
    private DiscoveryReadResult(Status status, DiscoverySnapshot snapshot) { this.status = status; this.snapshot = snapshot; }
    public static DiscoveryReadResult success(DiscoverySnapshot snapshot) { return new DiscoveryReadResult(Status.LOADED, Objects.requireNonNull(snapshot)); }
    public static DiscoveryReadResult failure(Status status) {
        if (Objects.requireNonNull(status) == Status.LOADED) throw new IllegalArgumentException("Success requires data");
        return new DiscoveryReadResult(status, null);
    }
    public Status getStatus() { return status; }
    public Optional<DiscoverySnapshot> getSnapshot() { return Optional.ofNullable(snapshot); }
}
