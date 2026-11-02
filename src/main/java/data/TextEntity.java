package data;

public abstract class TextEntity {
    public boolean sendByUser; // tells the text history if it was the user
                               // who sent the text or if the user received the text
    public String content;
    TextEntity (boolean sendByUser){
        this.sendByUser = sendByUser;
    }

    public abstract String returnContent();

}
