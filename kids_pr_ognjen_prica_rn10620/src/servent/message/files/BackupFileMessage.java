package servent.message.files;

import app.model.FileInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class BackupFileMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 6896706584092171165L;
    private final FileInfo fileInfo;
    private final int fileHash;

    public BackupFileMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, FileInfo fileInfo, int fileHash) {
        super(MessageType.BACKUP_FILE, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.fileInfo = fileInfo;
        this.fileHash = fileHash;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public int getFileHash() {
        return fileHash;
    }
}
