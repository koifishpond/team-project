package use_case.discovery;

/** Immutable observational counters, separate from the current learned vector. */
public final class EngagementMetrics {
    private final long impressions, firstOpens, activeMillis, likes, deliberateDislikes, confirmations;
    private final long matureImpressions, matureMatches, observedSessions;

    EngagementMetrics(long impressions, long firstOpens, long activeMillis, long likes, long deliberateDislikes,
                      long confirmations, long matureImpressions, long matureMatches, long observedSessions) {
        this.impressions = impressions;
        this.firstOpens = firstOpens;
        this.activeMillis = activeMillis;
        this.likes = likes;
        this.deliberateDislikes = deliberateDislikes;
        this.confirmations = confirmations;
        this.matureImpressions = matureImpressions;
        this.matureMatches = matureMatches;
        this.observedSessions = observedSessions;
    }

    public long impressions() { return impressions; }
    public long firstOpens() { return firstOpens; }
    public long activeMillis() { return activeMillis; }
    public long likes() { return likes; }
    public long deliberateDislikes() { return deliberateDislikes; }
    public long confirmations() { return confirmations; }
    public long matureImpressions() { return matureImpressions; }
    public long matureMatches() { return matureMatches; }
    public long observedSessions() { return observedSessions; }
    public long returnSessions() { return Math.max(0, observedSessions - 1); }
    public double openRate() { return rate(firstOpens, impressions); }
    public double matureMatchRate() { return rate(matureMatches, matureImpressions); }
    private static double rate(long count, long total) { return total == 0 ? 0 : count / (double) total; }
}
