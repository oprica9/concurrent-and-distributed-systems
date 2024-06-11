package servent.handler.ping_pong;

import app.AppConfig;
import app.ChordState;
import app.failure_detection.FailureDetector;
import app.model.ServentInfo;
import app.mutex.SuzukiKasamiMutex;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ping_pong.NewTokenHolderMessage;
import servent.message.ping_pong.YouWereSlowMessage;
import servent.message.util.MessageUtil;

public class NewTokenHolderHandler implements MessageHandler {

    private final Message clientMessage;
    private final FailureDetector failureDetector;

    public NewTokenHolderHandler(Message clientMessage, FailureDetector failureDetector) {
        this.clientMessage = clientMessage;
        this.failureDetector = failureDetector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.NEW_TOKEN_HOLDER) {
            AppConfig.timestampedErrorPrint("New token holder handler got a message that is not NEW_TOKEN_HOLDER");
            return;
        }
        synchronized (AppConfig.tokenLock) {
            NewTokenHolderMessage newTokenHolderMessage = (NewTokenHolderMessage) clientMessage;
            long declaredTime = newTokenHolderMessage.getDeclaredTime();

            AppConfig.timestampedStandardPrint("MY TOKEN: " + SuzukiKasamiMutex.TOKEN);
            if (SuzukiKasamiMutex.hasToken()) {
                if (!clientMessage.getSenderIpAddress().equals(AppConfig.myServentInfo.getIpAddress())
                        || clientMessage.getSenderPort() != AppConfig.myServentInfo.getListenerPort()) {
                    if (declaredTime < failureDetector.getMyDeclaredTime()) {
                        // They were faster
                        AppConfig.timestampedStandardPrint("I'm actually not the new token holder.");
                        SuzukiKasamiMutex.releaseToken();
                        failureDetector.resetDeclaredTime();
                    } else {
                        MessageUtil.sendMessage(new YouWereSlowMessage(
                                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                                clientMessage.getSenderIpAddress(), clientMessage.getSenderPort()
                        ));
                    }
                }
            } else {
                SuzukiKasamiMutex.reset();
                if ((!clientMessage.getSenderIpAddress().equals(AppConfig.myServentInfo.getIpAddress())
                        || clientMessage.getSenderPort() != AppConfig.myServentInfo.getListenerPort())
                        && !failureDetector.shouldBeTokenHolder()) {

                    int myChordId = AppConfig.myServentInfo.getChordId();
                    int clientChordId = ChordState.chordHash2(clientMessage.getSenderIpAddress(), clientMessage.getSenderPort());

                    for (int i = 0; i < AppConfig.chordState.getSuccessors().length; i++) {
                        ServentInfo succInfo = AppConfig.chordState.getSuccessors()[i];
                        if (succInfo == null) {
                            break;
                        }
                        int succChordId = succInfo.getChordId();
                        if (myChordId != succChordId && succChordId != clientChordId) {
                            MessageUtil.sendMessage(new NewTokenHolderMessage(
                                    clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                                    succInfo.getIpAddress(), succInfo.getListenerPort(),
                                    declaredTime
                            ));
                            break;
                        }
                    }
                }
            }

            // If we got this message it means that system restructuring has begun already, but we might not have received the message,
            // So we'll just handle that so that we may not accidentally set ourselves as a token handler after this message has been handled.
            failureDetector.setRefactored(true);
        }
    }
}
