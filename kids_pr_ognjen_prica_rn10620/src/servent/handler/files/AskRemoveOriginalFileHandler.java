package servent.handler.files;

import app.AppConfig;
import app.file_manager.FileManager;
import app.model.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.files.AskRemoveOriginalFileMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class AskRemoveOriginalFileHandler implements MessageHandler {

    private final Message clientMessage;
    private final FileManager fileManager;

    public AskRemoveOriginalFileHandler(Message clientMessage, FileManager fileManager) {
        this.clientMessage = clientMessage;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.ASK_REMOVE_ORIGINAL_FILE) {
            AppConfig.timestampedErrorPrint("Ask remove original file handler got a message that is not ASK_REMOVE_ORIGINAL_FILE");
            return;
        }

        AskRemoveOriginalFileMessage askRemoveOriginalFileMessage = (AskRemoveOriginalFileMessage) clientMessage;

        String filePath = askRemoveOriginalFileMessage.getFilePath();
        int ownerId = askRemoveOriginalFileMessage.getOwnerId();

        if (ownerId == AppConfig.myServentInfo.getChordId()) {
            int response = fileManager.removeOriginalFile(filePath);
            if (response == -2) {
                AppConfig.timestampedStandardPrint("Successfully removed file: " + filePath);
            }
        } else {
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(ownerId);

            // We have to handle the case when the original owner is dead
            // Simply compare the sender information with ours, if its equal
            // the message circled back to us and this means the owner is dead.

            if (!askRemoveOriginalFileMessage.hasVisited(AppConfig.myServentInfo.getChordId())) {
                List<Integer> visited = new ArrayList<>(askRemoveOriginalFileMessage.getVisited());

                visited.add(AppConfig.myServentInfo.getChordId());

                MessageUtil.sendMessage(new AskRemoveOriginalFileMessage(
                        clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                        nextNode.getIpAddress(), nextNode.getListenerPort(),
                        filePath, ownerId, visited
                ));
            }
        }
    }
}
