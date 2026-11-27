package use_case.ManageMatches.ListMatches;

public class ListMatchesInputData {
    private final int userId;

    public ListMatchesInputData(int userId){
        this.userId = userId;
    }

    public int getUserId() {
        return userId;
    }
}
