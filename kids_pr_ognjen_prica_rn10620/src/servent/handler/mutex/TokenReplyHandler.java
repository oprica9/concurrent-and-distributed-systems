package servent.handler.mutex;

import app.AppConfig;
import app.model.ServentInfo;
import app.mutex.SuzukiKasamiMutex;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.mutex.TokenReplyMessage;
import servent.message.util.MessageUtil;

public class TokenReplyHandler implements MessageHandler {

    private final Message clientMessage;

    public TokenReplyHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.TOKEN_REPLY) {
            AppConfig.timestampedErrorPrint("Token reply handler got a message that is not TOKEN_REPLY");
            return;
        }

        TokenReplyMessage replyMessage = (TokenReplyMessage) clientMessage;
        int requesterId = replyMessage.getRequesterId();

        if (requesterId == AppConfig.myServentInfo.getChordId()) {
            SuzukiKasamiMutex.setToken(replyMessage.getToken());
        } else {
            // If we didn't ask for it, propagate it
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(requesterId);

            MessageUtil.sendMessage(new TokenReplyMessage(
                    clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                    nextNode.getIpAddress(), nextNode.getListenerPort(),
                    replyMessage.getToken(), requesterId
            ));
        }
    }
}
