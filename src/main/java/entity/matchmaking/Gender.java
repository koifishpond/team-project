package entity.matchmaking;

public enum Gender {
    WOMAN("Woman"),
    MAN("Man"),
    NON_BINARY("Non-binary"),
    SELF_DESCRIBED("Self-described gender");
    private final String label;
    Gender(String label) { this.label = label; }
    public String getLabel() { return label; }
}
