package servent.message.mutex;

import app.mutex.Token;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class TokenReplyMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 7562190636791020687L;
    private final Token token;
    private final int requesterId;

    public TokenReplyMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, Token token, int requesterId) {
        super(MessageType.TOKEN_REPLY, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.token = token;
        this.requesterId = requesterId;
    }

    public Token getToken() {
        return token;
    }

    public int getRequesterId() {
        return requesterId;
    }

}
