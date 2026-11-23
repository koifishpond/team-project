package entity.matchmaking;

import java.util.Objects;

/** Supplementary assessment. Ranking currently assigns it zero influence. */
public final class ProfileAssessment {
    public enum Personality {
        UNKNOWN, ISTJ, ISFJ, INFJ, INTJ, ISTP, ISFP, INFP, INTP, ESTP, ESFP, ENFP, ENTP, ESTJ, ESFJ, ENFJ, ENTJ
    }

    private final int schemaVersion, rubricVersion;
    private final long profileRevision;
    private final String modelVersion;
    private final double effort;
    private final Personality personality;

    public ProfileAssessment(int schemaVersion, int rubricVersion, long profileRevision, String modelVersion,
                             double effort, Personality personality) {
        if (schemaVersion < 1 || rubricVersion < 1 || profileRevision < 1 || !Double.isFinite(effort) || effort < 0 || effort > 1
                || !Objects.requireNonNull(modelVersion).matches("[A-Za-z0-9._:/-]{1,100}")) {
            throw new IllegalArgumentException("Invalid assessment fields");
        }
        this.schemaVersion = schemaVersion;
        this.rubricVersion = rubricVersion;
        this.profileRevision = profileRevision;
        this.modelVersion = modelVersion;
        this.effort = effort;
        this.personality = Objects.requireNonNull(personality);
    }

    public int schemaVersion() { return schemaVersion; }
    public int rubricVersion() { return rubricVersion; }
    public long profileRevision() { return profileRevision; }
    public String modelVersion() { return modelVersion; }
    public double effort() { return effort; }
    public Personality personality() { return personality; }
}
