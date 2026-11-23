package entity.matchmaking;

public enum LocationArea {
    TORONTO("Toronto", 43.65, -79.38), MISSISSAUGA("Mississauga", 43.59, -79.64),
    BRAMPTON("Brampton", 43.68, -79.76), MARKHAM("Markham", 43.86, -79.34), VAUGHAN("Vaughan", 43.84, -79.50);
    private final String label;
    private final double latitude, longitude;
    LocationArea(String label, double latitude, double longitude) {
        this.label = label; this.latitude = latitude; this.longitude = longitude;
    }
    public String getLabel() { return label; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
