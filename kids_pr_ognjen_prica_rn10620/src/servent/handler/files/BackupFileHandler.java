package servent.handler.files;

import app.AppConfig;
import app.file_manager.FileManager;
import app.model.FileInfo;
import app.model.ServentInfo;
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
        FileInfo backupFile = backupFileMessage.getFileInfo();

        if (AppConfig.chordState.isKeyMine(fileHash)) {
            if (!fileManager.containsFile(backupFile)) {
                fileManager.addFile(backupFile);
                AppConfig.timestampedStandardPrint("Saving backup for file: " + backupFile.getPath() + ", backupId: " + backupFile.getBackupId());
            }

            // Increase the backupId
            backupFile = fileManager.getInfoWithIncrementedBackupId(backupFile);

            // Send to successor
            sendBackupMessage(backupFile, fileHash, AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort());
        } else {
            if (backupFile.getBackupId() > 0) {
                // Time to save
                if (AppConfig.myServentInfo.getChordId() != backupFile.getOwnerKey()) {
                    fileManager.addFile(backupFile);
                    AppConfig.timestampedStandardPrint("Saving backup for file: " + backupFile.getPath() + ", backupId: " + backupFile.getBackupId());
                }

                // Increase the backupId
                backupFile = fileManager.getInfoWithIncrementedBackupId(backupFile);

                // Send to successor if we didn't hit a backup limit
                if (backupFile.getBackupId() < FileManager.REPLICATION_FACTOR) {
                    sendBackupMessage(backupFile, fileHash, AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort());
                    AppConfig.timestampedStandardPrint("Forwarding backup for file: " + backupFile.getPath() + " to successor, backupId: " + backupFile.getBackupId());
                }
            } else {
                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(fileHash);
                sendBackupMessage(backupFile, fileHash, nextNode.getIpAddress(), nextNode.getListenerPort());
                AppConfig.timestampedStandardPrint("Forwarding backup for file: " + backupFile.getPath() + ", backupId: " + backupFile.getBackupId());
            }
        }
    }

    private void sendBackupMessage(FileInfo backupFile, int fileHash, String ip, int port) {
        MessageUtil.sendMessage(new BackupFileMessage(
                clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                ip, port,
                backupFile, fileHash
        ));
    }

}
