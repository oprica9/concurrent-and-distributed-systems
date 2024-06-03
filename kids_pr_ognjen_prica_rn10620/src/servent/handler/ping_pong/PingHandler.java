package servent.handler.ping_pong;

import app.AppConfig;
import app.failure_detection.FailureDetector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ping_pong.PongMessage;
import servent.message.util.MessageUtil;

public class PingHandler implements MessageHandler {

    private final Message clientMessage;
    private final FailureDetector failureDetector;

    public PingHandler(Message clientMessage, FailureDetector failureDetector) {
        this.clientMessage = clientMessage;
        this.failureDetector = failureDetector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.PING) {
            AppConfig.timestampedErrorPrint("Ping handler got a message that is not PING");
            return;
        }

        PongMessage pongMessage = new PongMessage(
                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                failureDetector.getDeadNodes()
        );
        MessageUtil.sendMessage(pongMessage);
    }
}
