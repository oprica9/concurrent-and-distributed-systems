package servent.message.dht;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class AskGetMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -8558031124520315033L;

    public AskGetMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, String text) {
        super(MessageType.ASK_GET, senderIpAddress, senderPort, receiverIpAddress, receiverPort, text);
    }
}
