package servent.message.ping_pong;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class CheckSusMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = -7853466808471924892L;

    private final String susIp;
    private final int susPort;

    public CheckSusMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, String susIp, int susPort) {
        super(MessageType.CHECK_SUS, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.susIp = susIp;
        this.susPort = susPort;
    }

    public String getSusIp() {
        return susIp;
    }

    public int getSusPort() {
        return susPort;
    }
}
