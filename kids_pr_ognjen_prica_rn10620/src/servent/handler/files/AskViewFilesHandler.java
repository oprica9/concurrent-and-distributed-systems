package servent.handler.files;

import app.AppConfig;
import app.ChordState;
import app.file_manager.FileManager;
import app.model.FileInfo;
import app.model.ServentInfo;
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

        AskViewFilesMessage askViewFilesMessage = (AskViewFilesMessage) clientMessage;

        String ip = askViewFilesMessage.getIp();
        int port = askViewFilesMessage.getPort();
        int ogKey = askViewFilesMessage.getOgKey();

        Map<String, FileInfo> fileMap = fileManager.getFilesIfIpPortMatches(ip, port, ogKey);

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
