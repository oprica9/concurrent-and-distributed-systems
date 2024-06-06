package servent.message.ping_pong;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class NewTokenHolderMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 7447273146220430737L;

    private final long declaredTime;

    public NewTokenHolderMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, long declaredTime) {
        super(MessageType.NEW_TOKEN_HOLDER, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.declaredTime = declaredTime;
    }

    public long getDeclaredTime() {
        return declaredTime;
    }
}
