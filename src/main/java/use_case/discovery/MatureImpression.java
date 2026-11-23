package use_case.discovery;

/** Outcomes observed within a complete follow-up window. No candidate or viewer identifiers. */
public final class MatureImpression {
    private final boolean opened, matched, disliked;

    public MatureImpression(boolean opened, boolean matched, boolean disliked) {
        this.opened = opened;
        this.matched = matched;
        this.disliked = disliked;
    }

    public boolean opened() { return opened; }
    public boolean matched() { return matched; }
    public boolean disliked() { return disliked; }
}
