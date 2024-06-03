package servent.handler.files;

import app.AppConfig;
import app.FileManager;
import app.model.ServentInfo;
import app.model.StoredFileInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.files.AskRemoveFileMessage;
import servent.message.files.AskRemoveOriginalFileMessage;
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

        String[] args = askRemoveFileMessage.getMessageText().split(",");

        String filePath = args[0];
        int fileHash;
        try {
            fileHash = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Ask remove file handler got an invalid argument: " + args[1]);
            return;
        }

        if (AppConfig.chordState.isKeyMine(fileHash)) {
            StoredFileInfo toRemove = fileManager.getFile(filePath);

            int ownerId = toRemove.getOwnerKey();
            if (ownerId != AppConfig.myServentInfo.getChordId()) {
                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(ownerId);

                MessageUtil.sendMessage(new AskRemoveOriginalFileMessage(
                        AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                        nextNode.getIpAddress(), nextNode.getListenerPort(),
                        toRemove.getPath(), ownerId
                ));
            }

            fileManager.deleteBackup(filePath);
            AppConfig.timestampedStandardPrint("Removing backup for file " + filePath);

            MessageUtil.sendMessage(new AskRemoveFileMessage(
                    clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                    AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort(),
                    filePath, fileHash, -1
            ));
        } else {
            if (askRemoveFileMessage.getBackupId() != -1) {
                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(fileHash);

                MessageUtil.sendMessage(new AskRemoveFileMessage(
                        clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                        nextNode.getIpAddress(), nextNode.getListenerPort(),
                        filePath, fileHash, 1));
                AppConfig.timestampedStandardPrint("Forwarding backup deletion for file: " + filePath);
            } else {
                if (fileManager.getFile(filePath).getOwnerKey() != AppConfig.myServentInfo.getChordId()) {
                    int backupId = fileManager.deleteBackup(filePath);
                    AppConfig.timestampedStandardPrint("Removing backup for file " + filePath);

                    if (backupId < FileManager.REPLICATION_FACTOR) {
                        MessageUtil.sendMessage(new AskRemoveFileMessage(
                                clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                                AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort(),
                                filePath, fileHash, -1
                        ));
                    }
                }
            }
        }
    }

}

