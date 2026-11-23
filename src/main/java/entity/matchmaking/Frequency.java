package entity.matchmaking;

public enum Frequency {
    NEVER("Never"),
    SOMETIMES("Sometimes"),
    REGULARLY("Regularly");
    private final String label;
    Frequency(String label) { this.label = label; }
    public String getLabel() { return label; }
}
