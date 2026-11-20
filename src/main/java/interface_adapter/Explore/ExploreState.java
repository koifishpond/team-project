package interface_adapter.Explore;

public class ExploreState {

    private final String name;
    private final int age;
    private final String bio;
    private final String interests;
    private final String fullBio;

    public ExploreState(String name, int age, String bio,
                        String interests, String fullBio) {
        this.name = name;
        this.age = age;
        this.bio = bio;
        this.interests = interests;
        this.fullBio = fullBio;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getBio() {
        return bio;
    }

    public String getInterests() {
        return interests;
    }

    public String getFullBio() {
        return fullBio;
    }

}
