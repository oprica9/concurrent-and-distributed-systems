package servent.handler.files;

import app.AppConfig;
import app.ChordState;
import app.FileManager;
import app.model.ServentInfo;
import app.model.StoredFileInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.files.AskViewFilesMessage;
import servent.message.files.TellViewFilesMessage;
import servent.message.util.MessageUtil;

import java.util.Map;

public class AskViewFilesHandler implements MessageHandler {

    private final Message clientMessage;
    private final FileManager fileManager;

    public AskViewFilesHandler(Message clientMessage, FileManager fileManager) {
        this.clientMessage = clientMessage;
        this.fileManager = fileManager;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.ASK_VIEW_FILES) {
            AppConfig.timestampedErrorPrint("Ask view files handler got a message that is not ASK_VIEW_FILES");
            return;
        }

        String[] msg = clientMessage.getMessageText().split(",");

        String[] ipPort = msg[0].split(":");
        String ip = ipPort[0];
        int port;
        int ogKey;
        try {
            port = Integer.parseInt(ipPort[1]);
            ogKey = Integer.parseInt(msg[1]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Could not parse port: " + ipPort[1]);
            return;
        }

        Map<String, StoredFileInfo> fileMap = fileManager.getFilesIfIpPortMatches(ip, port, ogKey);

        ServentInfo nextNode = fileMap != null
                ? AppConfig.chordState.getNextNodeForKey(ogKey)
                : AppConfig.chordState.getNextNodeForKey(ChordState.chordHash2(ip, port));

        Message newMessage = fileMap != null
                ? new TellViewFilesMessage(
                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                nextNode.getIpAddress(), nextNode.getListenerPort(),
                fileMap,
                ogKey)
                : new AskViewFilesMessage(
                clientMessage.getSenderIpAddress(),
                clientMessage.getSenderPort(),
                nextNode.getIpAddress(),
                nextNode.getListenerPort(),
                ip, port,
                ogKey);

        MessageUtil.sendMessage(newMessage);
    }
}
