package servent.message;

import java.io.Serial;

public class NewNodeMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 3899837286642127636L;

    public NewNodeMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort) {
        super(MessageType.NEW_NODE, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
    }
}
