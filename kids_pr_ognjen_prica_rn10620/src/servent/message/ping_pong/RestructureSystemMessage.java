package servent.message.ping_pong;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class RestructureSystemMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 2461312145789440613L;
    private final String deadIpAddress;
    private final int deadPort;

    public RestructureSystemMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, String deadIpAddress, int deadPort) {
        super(MessageType.RESTRUCTURE, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.deadIpAddress = deadIpAddress;
        this.deadPort = deadPort;
    }

    public String getDeadIpAddress() {
        return deadIpAddress;
    }

    public int getDeadPort() {
        return deadPort;
    }
}
