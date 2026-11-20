package entity.matchmaking;

public enum InterestTag {
    ART("Art"),
    BOARD_GAMES("Board games"),
    BOOKS("Books"),
    COOKING("Cooking"),
    FILM("Film"),
    FITNESS("Fitness"),
    GAMING("Gaming"),
    HIKING("Hiking"),
    MUSIC("Music"),
    SPORTS("Sports"),
    TRAVEL("Travel"),
    VOLUNTEERING("Volunteering");
    private final String label;
    InterestTag(String label) { this.label = label; }
    public String getLabel() { return label; }
}
