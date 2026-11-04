package data;
import java.time.LocalDateTime;

public abstract class TextEntity {
    public boolean sendByUser; // tells the text history if it was the user
                               // who sent the text or if the user received the text
    public LocalDateTime sentTime;
    public String content;
    TextEntity (boolean sendByUser){
        this.sendByUser = sendByUser;
    }

    public abstract String returnContent();

    public void timeStampInput (LocalDateTime sentTime){
        this.sentTime = sentTime; //since we have to save text history, we can't really set the sentTime as current time
    }
}
