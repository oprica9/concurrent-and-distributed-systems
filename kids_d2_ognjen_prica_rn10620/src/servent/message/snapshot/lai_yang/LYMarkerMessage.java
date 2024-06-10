package servent.message.snapshot.lai_yang;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class LYMarkerMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 388942509576636228L;

    public LYMarkerMessage(ServentInfo sender, ServentInfo receiver, int collectorId) {
        super(MessageType.LY_MARKER, sender, receiver, String.valueOf(collectorId));
    }
}
