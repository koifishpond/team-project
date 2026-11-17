package entity;

import java.time.LocalDateTime;

public abstract class TextEntity {
    public boolean sendByUser; // Indicates whether the user sent or received the text.
    public LocalDateTime sentTime;
    public String content;

    TextEntity(boolean sendByUser, LocalDateTime sentTime) {
        this.sendByUser = sendByUser;
        this.sentTime = sentTime;
    }

    public abstract String returnContent();

    public void timeStampInput(LocalDateTime sentTime) {
        this.sentTime = sentTime;
    }
}
