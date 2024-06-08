package servent.message.ping_pong;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;
import java.util.List;

public class PongMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 2742359599772737116L;
    private final List<String> deadServents;

    public PongMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, List<String> deadServents) {
        super(MessageType.PONG, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.deadServents = deadServents;
    }

    public List<String> getDeadServents() {
        return deadServents;
    }
}
