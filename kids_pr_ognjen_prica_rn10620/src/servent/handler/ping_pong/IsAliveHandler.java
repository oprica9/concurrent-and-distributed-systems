package servent.handler.ping_pong;

import app.AppConfig;
import app.ChordState;
import app.failure_detection.FailureDetector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ping_pong.IsAliveMessage;

public class IsAliveHandler implements MessageHandler {

    private final Message clientMessage;
    private final FailureDetector failureDetector;

    public IsAliveHandler(Message clientMessage, FailureDetector failureDetector) {
        this.clientMessage = clientMessage;
        this.failureDetector = failureDetector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.IS_ALIVE) {
            AppConfig.timestampedErrorPrint("Is alive handler got a message that is not IS_ALIVE");
            return;
        }

        IsAliveMessage isAliveMessage = (IsAliveMessage) clientMessage;
        String aliveIp = isAliveMessage.getAliveIp();
        int alivePort = isAliveMessage.getAlivePort();

        failureDetector.updateLastResponseTime(ChordState.chordHash2(aliveIp, alivePort));
    }
}
