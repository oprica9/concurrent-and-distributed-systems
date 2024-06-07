package servent.handler;

import app.AppConfig;
import app.failure_detection.FailureDetector;
import app.file_manager.FileManager;
import app.model.ServentInfo;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.UpdateMessage;
import servent.message.mutex.UnlockMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class UpdateHandler implements MessageHandler {

    private final Message updateMessage;
    private final FailureDetector failureDetector;

    public UpdateHandler(Message updateMessage, FailureDetector failureDetector, FileManager fileManager) {
        this.updateMessage = updateMessage;
        this.failureDetector = failureDetector;
    }

    @Override
    public void run() {
        if (updateMessage.getMessageType() != MessageType.UPDATE) {
            AppConfig.timestampedErrorPrint("Update message handler got message that is not UPDATE");
            return;
        }

        if (updateMessage.getSenderIpAddress().equals(AppConfig.myServentInfo.getIpAddress()) &&
                updateMessage.getSenderPort() == AppConfig.myServentInfo.getListenerPort()) {
            // If the message got back to us
            String messageText = updateMessage.getMessageText();
            String[] ipPorts = messageText.split(",");

            List<ServentInfo> allNodes = new ArrayList<>();
            for (String ipPort : ipPorts) {
                String ip = ipPort.split(":")[0];
                int port = Integer.parseInt(ipPort.split(":")[1]);

                allNodes.add(new ServentInfo(ip, port));
            }

            AppConfig.chordState.addNodes(allNodes);
            failureDetector.updateNodeList();

            // Critical section end
            MessageUtil.sendMessage(new UnlockMessage(
                    AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort()
            ));
        } else {
            // If we are not the original sender
            ServentInfo newNodeInfo = new ServentInfo(updateMessage.getSenderIpAddress(), updateMessage.getSenderPort());
            List<ServentInfo> newNodes = new ArrayList<>();
            newNodes.add(newNodeInfo);

            AppConfig.chordState.addNodes(newNodes);

            // Update the failure detector
            failureDetector.updateNodeList();

            Message nextUpdate = getUpdateMessage();
            MessageUtil.sendMessage(nextUpdate);
        }
    }

    private Message getUpdateMessage() {
        String newMessageText;
        String myIpPort = AppConfig.myServentInfo.getIpAddress() + ":" + AppConfig.myServentInfo.getListenerPort();

        if (updateMessage.getMessageText().isEmpty()) {
            newMessageText = myIpPort;
        } else {
            newMessageText = updateMessage.getMessageText() + "," + myIpPort;
        }

        return new UpdateMessage(
                updateMessage.getSenderIpAddress(),
                updateMessage.getSenderPort(),
                AppConfig.chordState.getNextNodeIpAddress(),
                AppConfig.chordState.getNextNodePort(),
                newMessageText
        );
    }
}

