package servent.message.ping_pong;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class PongMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 2742359599772737116L;

    public PongMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, String deadNodes) {
        super(MessageType.PONG, senderIpAddress, senderPort, receiverIpAddress, receiverPort, deadNodes);
    }
}
