package entity;
import java.time.LocalDateTime;

public abstract class TextEntity {
    public boolean sendByUser; // tells the text history if it was the user
                               // who sent the text or if the user received the text
    public LocalDateTime sentTime;
    public String content;
    TextEntity (boolean sendByUser, LocalDateTime sentTime){
        this.sendByUser = sendByUser;
        this.sentTime = sentTime;
    }

    public abstract String returnContent();

    public void timeStampInput (LocalDateTime sentTime){
        this.sentTime = sentTime; //backup setter just in case
    }
}
