package servent.handler.ping_pong;

import app.AppConfig;
import app.ChordState;
import app.failure_detection.FailureDetector;
import app.model.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ping_pong.PongMessage;
import servent.message.ping_pong.RestructureSystemMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

public class PongHandler implements MessageHandler {
    private final Message clientMessage;
    private final FailureDetector failureDetector;

    public PongHandler(Message clientMessage, FailureDetector failureDetector) {
        this.clientMessage = clientMessage;
        this.failureDetector = failureDetector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.PONG) {
            AppConfig.timestampedErrorPrint("Pong handler got a message that is not PONG");
            return;
        }
        failureDetector.updateLastResponseTime(ChordState.chordHash2(clientMessage.getSenderIpAddress(), clientMessage.getSenderPort()));

        PongMessage pongMessage = (PongMessage) clientMessage;
        List<String> deadNodes = pongMessage.getDeadNodes();

        if (!deadNodes.isEmpty()) {
            // our buddies buddy has died

            List<ServentInfo> deadInfos = new ArrayList<>();

            for (String ipPort : deadNodes) {
                String[] split = ipPort.split(":");
                String ip = split[0];
                int port;
                try {
                    port = Integer.parseInt(split[1]);
                } catch (NumberFormatException e) {
                    AppConfig.timestampedErrorPrint("Pong handler could not parse element from list in message: " + ipPort);
                    return;
                }

                ServentInfo deadInfo = new ServentInfo(ip, port);
                deadInfos.add(deadInfo);
            }

            AppConfig.timestampedStandardPrint("Restructuring system from PONG");
            AppConfig.chordState.removeNodes(deadInfos);

            for (ServentInfo deadInfo : deadInfos) {
                for (ServentInfo succ : AppConfig.chordState.getSuccessors()) {
                    if (!succ.getIpAddress().equals(deadInfo.getIpAddress()) || succ.getListenerPort() != deadInfo.getListenerPort()) {
                        if (!succ.getIpAddress().equals(clientMessage.getSenderIpAddress()) && succ.getListenerPort() != clientMessage.getSenderPort()) {
                            MessageUtil.sendMessage(new RestructureSystemMessage(
                                    clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                                    AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort(),
                                    deadInfo.getIpAddress(), deadInfo.getListenerPort()
                            ));
                            break;
                        }
                    }
                }
            }

        }

    }
}
