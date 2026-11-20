package entity.matchmaking;

public enum AudienceMode {
    UNSET("Unset"),
    ANY("Any"),
    SELECTED("Selected");
    private final String label;
    AudienceMode(String label) { this.label = label; }
    public String getLabel() { return label; }
}
