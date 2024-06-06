package servent.message.ping_pong;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class UAliveMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = -6654604240247330948L;

    private final int concernedId;

    public UAliveMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, int concernedId) {
        super(MessageType.U_ALIVE, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.concernedId = concernedId;
    }

    public int getConcernedId() {
        return concernedId;
    }
}
