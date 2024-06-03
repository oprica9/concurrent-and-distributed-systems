package servent.message.ping_pong;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class PingMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = -5638617007208022948L;

    public PingMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort) {
        super(MessageType.PING, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
    }
}
