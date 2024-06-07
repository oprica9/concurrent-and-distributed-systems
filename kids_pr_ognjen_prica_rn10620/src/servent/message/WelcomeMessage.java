package servent.message;

import app.model.FileInfo;

import java.io.Serial;
import java.util.Map;

public class WelcomeMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -8981406250652693908L;

    private final Map<Integer, Integer> values;
    private final Map<String, FileInfo> files;

    public WelcomeMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, Map<Integer, Integer> values, Map<String, FileInfo> files) {
        super(MessageType.WELCOME, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.values = values;
        this.files = files;
    }

    public Map<Integer, Integer> getValues() {
        return values;
    }

    public Map<String, FileInfo> getFiles() {
        return files;
    }
}
