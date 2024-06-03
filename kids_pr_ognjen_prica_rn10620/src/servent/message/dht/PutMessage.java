package servent.message.dht;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class PutMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 5163039209888734276L;

    public PutMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, int key, int value) {
        super(MessageType.PUT, senderIpAddress,senderPort, receiverIpAddress, receiverPort, key + ":" + value);
    }
}
