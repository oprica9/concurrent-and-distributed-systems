package servent.message.ping_pong;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class PingMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -1934709147043909111L;

    public PingMessage(ServentInfo senderInfo, ServentInfo receiverInfo) {
        super(MessageType.PING, senderInfo, receiverInfo, "PING");

    }

}
