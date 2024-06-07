package servent.message.files;

import app.model.FileInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;
import java.util.Map;

public class TellViewFilesMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = -2795629108401086218L;
    private final int ogKey;
    private final Map<String, FileInfo> fileMap;

    public TellViewFilesMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, Map<String, FileInfo> fileMap, int ogKey) {
        super(MessageType.TELL_VIEW_FILES, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.ogKey = ogKey;
        this.fileMap = fileMap;
    }

    public int getOgKey() {
        return ogKey;
    }

    public Map<String, FileInfo> getFileMap() {
        return fileMap;
    }
}
