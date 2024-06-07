package servent.handler.files;

import app.AppConfig;
import app.file_manager.FileManager;
import app.model.FileInfo;
import app.model.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.files.AskRemoveFileMessage;
import servent.message.util.MessageUtil;

public class AskRemoveFileHandler implements MessageHandler {

    private final Message clientMessage;
    private final FileManager fileManager;

    public AskRemoveFileHandler(Message clientMessage, FileManager fileManager) {
        this.clientMessage = clientMessage;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.ASK_REMOVE_FILE) {
            AppConfig.timestampedErrorPrint("Ask remove file handler got a message that is not ASK_REMOVE_FILE");
            return;
        }

        AskRemoveFileMessage askRemoveFileMessage = (AskRemoveFileMessage) clientMessage;

        String filePath = askRemoveFileMessage.getFilePath();
        int fileHash = askRemoveFileMessage.getFileHash();

        // Is first backup
        if (AppConfig.chordState.isKeyMine(fileHash)) {
            FileInfo toRemove = fileManager.getFile(filePath);

            fileManager.askRemoveOriginalFile(toRemove);
            fileManager.deleteBackup(filePath);

            if (toRemove.getOwnerKey() == AppConfig.myServentInfo.getChordId()) {
                AppConfig.timestampedStandardPrint("Removing original file " + filePath + ", backupId: " + toRemove.getBackupId());
            } else {
                AppConfig.timestampedStandardPrint("Removing backup for file " + filePath + ", backupId: " + toRemove.getBackupId());
            }

            MessageUtil.sendMessage(new AskRemoveFileMessage(
                    clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                    AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort(),
                    filePath, fileHash, 1, toRemove.getOwnerKey()
            ));
        } else {
            int backupId = askRemoveFileMessage.getBackupId();
            int ownerId = askRemoveFileMessage.getOwnerId();
            if (backupId > 0) {
                if (backupId < FileManager.REPLICATION_FACTOR) {

                    if (askRemoveFileMessage.getOwnerId() != AppConfig.myServentInfo.getChordId()) {
                        backupId = fileManager.deleteBackup(filePath);
                        AppConfig.timestampedStandardPrint("Removing backup for file " + filePath + ", backupId: " + backupId);
                    }

                    MessageUtil.sendMessage(new AskRemoveFileMessage(
                            clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                            AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort(),
                            filePath, fileHash, backupId + 1, ownerId
                    ));
                }

            } else {
                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(fileHash);

                MessageUtil.sendMessage(new AskRemoveFileMessage(
                        clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                        nextNode.getIpAddress(), nextNode.getListenerPort(),
                        filePath, fileHash, backupId, ownerId));
                AppConfig.timestampedStandardPrint("Forwarding backup deletion for file: " + filePath + ", backupId: -1");
            }
        }
    }

}

