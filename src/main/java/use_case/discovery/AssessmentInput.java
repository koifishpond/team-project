package use_case.discovery;

import java.util.List;
import java.util.Objects;

/** Only checked-in synthetic scenarios can reach the provider adapter. */
public final class AssessmentInput {
    public enum Fixture {
        SPARSE("I like music.", List.of("MUSIC")),
        DETAILED("I play clarinet in a community band and repair bicycles at a local workshop on Saturdays.",
                List.of("MUSIC", "VOLUNTEERING"));

        private final String biography;
        private final List<String> interests;

        Fixture(String biography, List<String> interests) {
            this.biography = biography;
            this.interests = interests;
        }
    }

    public static final int SCHEMA_VERSION = 1;
    public static final int RUBRIC_VERSION = 1;
    private final Fixture fixture;
    private final long revision;

    public AssessmentInput(Fixture fixture, long revision) {
        this.fixture = Objects.requireNonNull(fixture);
        if (revision < 1) throw new IllegalArgumentException("Invalid profile revision");
        this.revision = revision;
    }

    public Fixture fixture() { return fixture; }
    public long revision() { return revision; }
    public String biography() { return fixture.biography; }
    public List<String> interests() { return fixture.interests; }
}
