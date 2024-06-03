package servent.handler.files;

import app.AppConfig;
import app.FileManager;
import app.model.ServentInfo;
import app.model.StoredFileInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.files.BackupFileMessage;
import servent.message.util.MessageUtil;

public class BackupFileHandler implements MessageHandler {

    private final Message clientMessage;
    private final FileManager fileManager;

    public BackupFileHandler(Message clientMessage, FileManager fileManager) {
        this.clientMessage = clientMessage;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.BACKUP_FILE) {
            AppConfig.timestampedErrorPrint("Backup file handler got a message that is not BACKUP_FILE");
            return;
        }

        BackupFileMessage backupFileMessage = (BackupFileMessage) clientMessage;
        int fileHash = backupFileMessage.getFileHash();
        StoredFileInfo backupFile = backupFileMessage.getStoredFileInfo();

        if (AppConfig.chordState.isKeyMine(fileHash)) {
            if (!fileManager.containsFile(backupFile)) {
                fileManager.addFile(backupFile);
                AppConfig.timestampedStandardPrint("Saving backup for file: " + backupFile.getPath());
            }

            // Increase the backupId
            backupFile = new StoredFileInfo(backupFile.getPath(), backupFile.getFileContent(), backupFile.getVisibility(), backupFile.getOwnerKey(), backupFile.getBackupId() + 1);

            // Send to successor
            MessageUtil.sendMessage(new BackupFileMessage(
                    clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                    AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort(),
                    backupFile, fileHash
            ));

        } else {
            if (backupFile.getBackupId() > 0) {
                // Time to save
                if (AppConfig.myServentInfo.getChordId() != backupFile.getOwnerKey()) {
                    fileManager.addFile(backupFile);
                    AppConfig.timestampedStandardPrint("Saving backup for file: " + backupFile.getPath());
                }

                // Increase the backupId
                backupFile = new StoredFileInfo(backupFile.getPath(), backupFile.getFileContent(), backupFile.getVisibility(), backupFile.getOwnerKey(), backupFile.getBackupId() + 1);

                // Send to successor (we would like 3 backups)
                if (backupFile.getBackupId() < FileManager.REPLICATION_FACTOR) {
                    MessageUtil.sendMessage(new BackupFileMessage(
                            clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                            AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort(),
                            backupFile, fileHash
                    ));
                    AppConfig.timestampedStandardPrint("Forwarding backup for file: " + backupFile.getPath() + " to successor.");
                }
            } else {
                // Find the node that should hold it
                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(fileHash);

                MessageUtil.sendMessage(new BackupFileMessage(
                        clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                        nextNode.getIpAddress(), nextNode.getListenerPort(),
                        backupFile, fileHash
                ));
                AppConfig.timestampedStandardPrint("Forwarding backup for file: " + backupFile.getPath());
            }
        }
    }

}
