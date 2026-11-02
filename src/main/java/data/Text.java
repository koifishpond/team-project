package data;

public class Text extends TextEntity {
    public Text(boolean sendByUser, String text) {
        super(sendByUser);
        this.content = text;
    }

    @Override
    public String returnContent() {
        return content;
    }
}
