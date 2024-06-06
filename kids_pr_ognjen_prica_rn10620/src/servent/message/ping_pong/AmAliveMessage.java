package servent.message.ping_pong;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class AmAliveMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 3080352612142086357L;

    private final int concernedId;

    public AmAliveMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, int concernedId) {
        super(MessageType.AM_ALIVE, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.concernedId = concernedId;
    }

    public int getConcernedId() {
        return concernedId;
    }
}
