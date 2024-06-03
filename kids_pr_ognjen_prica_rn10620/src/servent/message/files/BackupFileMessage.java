package servent.message.files;

import app.model.StoredFileInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class BackupFileMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 6896706584092171165L;
    private final StoredFileInfo storedFileInfo;
    private final int fileHash;

    public BackupFileMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, StoredFileInfo storedFileInfo, int fileHash) {
        super(MessageType.BACKUP_FILE, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.storedFileInfo = storedFileInfo;
        this.fileHash = fileHash;
    }

    public StoredFileInfo getStoredFileInfo() {
        return storedFileInfo;
    }

    public int getFileHash() {
        return fileHash;
    }
}
