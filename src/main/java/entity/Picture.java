package entity;

import java.time.LocalDateTime;

public class Picture extends TextEntity {

    public Picture(boolean sendByUser, String pictureReferenceAddress, LocalDateTime sentTime) {
        super(sendByUser, sentTime);
        this.content = pictureReferenceAddress;
    }

    @Override
    public String returnContent() {
        return content;
    }
}
