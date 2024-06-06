package servent.handler.ping_pong;

import app.AppConfig;
import app.failure_detection.FailureDetector;
import app.model.ServentInfo;
import app.mutex.SuzukiKasamiMutex;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ping_pong.RestructureSystemMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class RestructureSystemHandler implements MessageHandler {

    private final Message clientMessage;
    private final FailureDetector failureDetector;

    public RestructureSystemHandler(Message clientMessage, FailureDetector failureDetector) {
        this.clientMessage = clientMessage;
        this.failureDetector = failureDetector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.RESTRUCTURE) {
            AppConfig.timestampedErrorPrint("Restructure system handler got a message that is not RESTRUCTURE");
            return;
        }

        String[] args = clientMessage.getMessageText().split(":");
        if (args.length != 2) {
            AppConfig.timestampedErrorPrint("Invalid arguments for a system restructure request.");
            return;
        }

        String ip = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Invalid arguments for a system restructure request.");
            return;
        }

        List<ServentInfo> deadServents = new ArrayList<>();
        ServentInfo deadServent = new ServentInfo(ip, port);

        // I should also try to become the token holder
        // But in case that the other node was too fast and I didn't become the token holder before
        // restructuring the system, I set the flag to true, so I know not to propagate the NEW_TOKEN_HOLDER message
        // when it arrives
        failureDetector.setShouldBeTokenHolder(deadServent.getChordId() == AppConfig.chordState.getSuccessors()[0].getChordId() || deadServent.getChordId() == AppConfig.chordState.getPredecessor().getChordId());

        deadServents.add(deadServent);
        AppConfig.timestampedStandardPrint("Restructuring system...\nRemoving " + deadServents);
        AppConfig.chordState.removeNodes(deadServents);
        failureDetector.updateNodeList();
        failureDetector.setRefactored(false);

        // If it ain't moi, send on
        if (!clientMessage.getSenderIpAddress().equals(AppConfig.myServentInfo.getIpAddress()) ||
                clientMessage.getSenderPort() != AppConfig.myServentInfo.getListenerPort()) {

            for (ServentInfo succ : AppConfig.chordState.getSuccessors()) {
                if (!succ.getIpAddress().equals(ip) || succ.getListenerPort() != port) {
                    MessageUtil.sendMessage(new RestructureSystemMessage(
                            clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                            AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort(),
                            ip, port
                    ));
                    break;
                }
            }
        }
    }
}
