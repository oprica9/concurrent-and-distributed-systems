package servent.message.files;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class AskRemoveFileMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 662551018842811949L;
    private final int backupId;

    public AskRemoveFileMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, String filePath, int fileHash, int backupId) {
        super(MessageType.ASK_REMOVE_FILE, senderIpAddress, senderPort, receiverIpAddress, receiverPort, filePath + "," + fileHash);
        this.backupId = backupId;
    }

    public int getBackupId() {
        return backupId;
    }
}
