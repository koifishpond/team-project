package entity;

import java.time.LocalDateTime;

public class Text extends TextEntity {
    public Text(boolean sendByUser, String text, LocalDateTime sentTime) {
        super(sendByUser, sentTime);
        this.content = text;
    }

    @Override
    public String returnContent() {
        return content;
    }
}
