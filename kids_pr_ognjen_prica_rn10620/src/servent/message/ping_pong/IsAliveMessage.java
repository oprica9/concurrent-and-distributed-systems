package servent.message.ping_pong;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class IsAliveMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 4910313518748752842L;

    private final String aliveIp;
    private final int alivePort;

    public IsAliveMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, String aliveIp, int alivePort) {
        super(MessageType.IS_ALIVE, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.aliveIp = aliveIp;
        this.alivePort = alivePort;
    }

    public String getAliveIp() {
        return aliveIp;
    }

    public int getAlivePort() {
        return alivePort;
    }
}
