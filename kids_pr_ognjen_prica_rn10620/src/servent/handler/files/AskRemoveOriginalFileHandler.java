package servent.handler.files;

import app.AppConfig;
import app.FileManager;
import app.model.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.files.AskRemoveOriginalFileMessage;
import servent.message.util.MessageUtil;

import java.util.Arrays;

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

        String[] args = clientMessage.getMessageText().split(",");

        if (args.length != 2) {
            AppConfig.timestampedErrorPrint("Ask remove original file handler received invalid arguments: " + Arrays.toString(args));
            return;
        }


        String filePath = args[0];
        int ownerId;
        try {
            ownerId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Ask remove file handler got an invalid ownerId: " + args[1]);
            return;
        }

        if (ownerId == AppConfig.myServentInfo.getChordId()) {
            int response = fileManager.removeOriginalFile(filePath);
            if (response == -2) {
                AppConfig.timestampedStandardPrint("Successfully removed file: " + filePath);
            }
        } else {
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(ownerId);

            MessageUtil.sendMessage(new AskRemoveOriginalFileMessage(
                    clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                    nextNode.getIpAddress(), nextNode.getListenerPort(),
                    filePath, ownerId
            ));
        }
    }
}
