package servent.message.ping_pong;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class YouWereSlowMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 689337360825617864L;

    public YouWereSlowMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort) {
        super(MessageType.YOU_WERE_SLOW, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
    }
}
