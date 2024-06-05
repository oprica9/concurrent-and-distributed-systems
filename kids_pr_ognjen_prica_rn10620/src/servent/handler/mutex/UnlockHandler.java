package servent.handler.mutex;

import app.AppConfig;
import app.mutex.SuzukiKasamiMutex;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class UnlockHandler implements MessageHandler {

    private final Message clientMessage;

    public UnlockHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.UNLOCK) {
            AppConfig.timestampedErrorPrint("Unlock handler got a message that is not UNLOCK");
            return;
        }

        SuzukiKasamiMutex.signalWelcomeResponseReceived();
    }
}
