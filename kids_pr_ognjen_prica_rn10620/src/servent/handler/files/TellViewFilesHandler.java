package servent.handler.files;

import app.AppConfig;
import app.FileManager;
import app.model.ServentInfo;
import app.model.StoredFileInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.files.TellViewFilesMessage;
import servent.message.util.MessageUtil;

import java.util.Map;

public class TellViewFilesHandler implements MessageHandler {

    private final Message clientMessage;

    public TellViewFilesHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.TELL_VIEW_FILES) {
            AppConfig.timestampedErrorPrint("Tell view files handler got a message that is not TELL_VIEW_FILES");
        }

        TellViewFilesMessage tellViewFilesMessage = (TellViewFilesMessage) clientMessage;

        int ogKey;
        try {
            ogKey = Integer.parseInt(tellViewFilesMessage.getMessageText());
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Error parsing the original sender key: " + e);
            return;
        }

        if (AppConfig.chordState.isKeyMine(ogKey)) {
            Map<String, StoredFileInfo> fileMap = tellViewFilesMessage.getFileMap();
            AppConfig.timestampedStandardPrint("Received file data from: " + clientMessage.getSenderIpAddress() + ":" + clientMessage.getSenderPort());
            FileManager.printFiles(fileMap);
        } else {
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(ogKey);
            Message tell = new TellViewFilesMessage(
                    AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                    nextNode.getIpAddress(), nextNode.getListenerPort(),
                    tellViewFilesMessage.getFileMap(),
                    ogKey
            );
            MessageUtil.sendMessage(tell);
        }
    }
}

