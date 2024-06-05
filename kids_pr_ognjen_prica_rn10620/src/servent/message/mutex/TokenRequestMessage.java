package servent.message.mutex;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class TokenRequestMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 6671008879259362696L;
    private final int j;
    /**
     * n (n = 1, 2, ...) is a sequence number that indicates that site Sj is requesting its nth CS execution.
     */
    private final int n;

    public TokenRequestMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, int j, int n) {
        super(MessageType.TOKEN_REQUEST, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.j = j;
        this.n = n;
    }

    public int getJ() {
        return j;
    }

    public int getN() {
        return n;
    }
}
