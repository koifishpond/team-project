package entity.matchmaking;

public enum PreferenceMode {
    SOFT("Soft"),
    HARD("Hard");
    private final String label;
    PreferenceMode(String label) { this.label = label; }
    public String getLabel() { return label; }
}
