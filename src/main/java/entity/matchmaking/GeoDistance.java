package entity.matchmaking;

/** Integer metres shared by eligibility, scoring and presentation. */
public final class GeoDistance {
    private GeoDistance() { }
    public static int between(LocationArea a, LocationArea b) {
        return metres(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    }
    public static int metres(double lat1, double lon1, double lat2, double lon2) {
        if (!Double.isFinite(lat1) || !Double.isFinite(lat2) || !Double.isFinite(lon1) || !Double.isFinite(lon2)
                || Math.abs(lat1) > 90 || Math.abs(lat2) > 90 || Math.abs(lon1) > 180 || Math.abs(lon2) > 180)
            throw new IllegalArgumentException("Coordinates");
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double a = Math.pow(Math.sin((p2-p1)/2), 2) + Math.cos(p1)*Math.cos(p2)*Math.pow(Math.sin(Math.toRadians(lon2-lon1)/2), 2);
        return (int) Math.round(2 * 6371000 * Math.asin(Math.sqrt(Math.max(0, Math.min(1, a)))));
    }
}
