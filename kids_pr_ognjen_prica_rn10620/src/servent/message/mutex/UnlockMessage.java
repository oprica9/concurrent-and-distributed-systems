package servent.message.mutex;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class UnlockMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -9202235614282058660L;

    public UnlockMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort) {
        super(MessageType.UNLOCK, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
    }

}
