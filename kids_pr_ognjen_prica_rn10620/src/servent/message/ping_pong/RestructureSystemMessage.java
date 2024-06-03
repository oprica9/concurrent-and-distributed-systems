package servent.message.ping_pong;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class RestructureSystemMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 2461312145789440613L;

    public RestructureSystemMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, String deadIpAddress, int deadPort) {
        super(MessageType.RESTRUCTURE, senderIpAddress, senderPort, receiverIpAddress, receiverPort, deadIpAddress + ":" + deadPort);
    }
}
