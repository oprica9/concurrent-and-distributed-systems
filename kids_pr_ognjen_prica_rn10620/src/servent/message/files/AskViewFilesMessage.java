package servent.message.files;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class AskViewFilesMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 7225191140209597882L;

    private final String ip;
    private final int port;
    private final int ogKey;

    public AskViewFilesMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, String ip, int port, int ogKey) {
        super(MessageType.ASK_VIEW_FILES, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.ip = ip;
        this.port = port;
        this.ogKey = ogKey;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getOgKey() {
        return ogKey;
    }
}
