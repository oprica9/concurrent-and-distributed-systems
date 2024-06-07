package servent.message.files;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class AskRemoveFileMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 662551018842811949L;
    private final String filePath;
    private final int fileHash;
    private final int backupId;
    private final int ownerId;

    public AskRemoveFileMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, String filePath, int fileHash, int backupId, int ownerId) {
        super(MessageType.ASK_REMOVE_FILE, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.filePath = filePath;
        this.fileHash = fileHash;
        this.backupId = backupId;
        this.ownerId = ownerId;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getFileHash() {
        return fileHash;
    }

    public int getBackupId() {
        return backupId;
    }

    public int getOwnerId() {
        return ownerId;
    }
}
