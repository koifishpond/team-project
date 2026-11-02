package data;
import java.util.ArrayList;
import java.util.List;

public class TextHistoryList {
    public List<TextEntity> textHistory = new ArrayList<>();

    public void addNewTextToHistory (TextEntity textObject) {
        textHistory.add(textObject);
    }
}
