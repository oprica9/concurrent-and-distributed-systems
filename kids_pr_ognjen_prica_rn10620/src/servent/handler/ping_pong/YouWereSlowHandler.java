package servent.handler.ping_pong;

import app.AppConfig;
import app.failure_detection.FailureDetector;
import app.mutex.SuzukiKasamiMutex;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class YouWereSlowHandler implements MessageHandler {

    private final Message clientMessage;
    private final FailureDetector failureDetector;

    public YouWereSlowHandler(Message clientMessage, FailureDetector failureDetector) {
        this.clientMessage = clientMessage;
        this.failureDetector = failureDetector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.YOU_WERE_SLOW) {
            AppConfig.timestampedErrorPrint("You were slow handler got a message that is not NEW_TOKEN_HOLDER");
            return;
        }

        synchronized (AppConfig.tokenLock) {
            SuzukiKasamiMutex.reset();
            SuzukiKasamiMutex.releaseToken();
            failureDetector.resetDeclaredTime();
        }
    }
}
