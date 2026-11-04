package data;

public class Picture extends TextEntity {

    public Picture(boolean sendByUser, String pictureReferenceAddress) {
        super(sendByUser);
        this.content = pictureReferenceAddress;
    }

    @Override
    public String returnContent() {
        return content;
    }
}
