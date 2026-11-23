package entity.matchmaking;

public enum DiscoveryStatus {
    DRAFT("Draft"),
    ACTIVE("Active"),
    PAUSED("Paused");
    private final String label;
    DiscoveryStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
