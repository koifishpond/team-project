package use_case.discovery;

public interface DiscoveryDataAccessInterface {
    DiscoveryReadResult load(ResolvedViewer viewer, java.time.LocalDate effectiveDate);
}
