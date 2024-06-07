package servent.message.ping_pong;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;
import java.util.List;

public class PongMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 2742359599772737116L;
    private final List<String> deadNodes;

    public PongMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, List<String> deadNodes) {
        super(MessageType.PONG, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.deadNodes = deadNodes;
    }

    public List<String> getDeadNodes() {
        return deadNodes;
    }
}
