package servent.message.snapshot.chandy_lamport;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class CLMarkerMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -3114137381491356339L;

    public CLMarkerMessage(ServentInfo sender, ServentInfo receiver, int collectorId) {
        super(MessageType.CL_MARKER, sender, receiver, String.valueOf(collectorId));
    }
}
