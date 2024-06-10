package servent.message.snapshot.li;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class LiMarkerMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 3129823407444745171L;

    public LiMarkerMessage(ServentInfo sender, ServentInfo receiver) {
        super(MessageType.LI_MARKER, sender, receiver);
    }
}
