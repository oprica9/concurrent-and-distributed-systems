package servent.message.files;

import app.model.StoredFileInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;
import java.util.Map;

public class TellViewFilesMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = -2795629108401086218L;
    private final Map<String, StoredFileInfo> fileMap;

    public TellViewFilesMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, Map<String, StoredFileInfo> fileMap, int ogKey) {
        super(MessageType.TELL_VIEW_FILES, senderIpAddress, senderPort, receiverIpAddress, receiverPort, String.valueOf(ogKey));
        this.fileMap = fileMap;
    }

    public Map<String, StoredFileInfo> getFileMap() {
        return fileMap;
    }
}
