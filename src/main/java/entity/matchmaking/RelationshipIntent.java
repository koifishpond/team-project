package entity.matchmaking;

public enum RelationshipIntent {
    LONG_TERM("Long-term"),
    SHORT_TERM("Short-term"),
    OPEN_TO_EITHER("Open to either"),
    FIGURING_IT_OUT("Figuring it out");
    private final String label;
    RelationshipIntent(String label) { this.label = label; }
    public String getLabel() { return label; }
}
