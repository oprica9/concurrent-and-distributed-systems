package servent.handler.mutex;

import app.AppConfig;
import app.model.ServentInfo;
import app.mutex.SuzukiKasamiMutex;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.mutex.TokenReplyMessage;
import servent.message.mutex.TokenRequestMessage;
import servent.message.util.MessageUtil;

public class TokenRequestHandler implements MessageHandler {

    private final Message clientMessage;

    public TokenRequestHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.TOKEN_REQUEST) {
            AppConfig.timestampedErrorPrint("Token request handler got a message that is not TOKEN_REQUEST");
            return;
        }

        TokenRequestMessage requestMessage = (TokenRequestMessage) clientMessage;
        int i = requestMessage.getJ();
        int n = requestMessage.getN();

        // When a site Sj receives this message, it sets RNj[i] to max(RNj[i], sn).
        // When a site Si receives a REQUEST(j, n) message, the request is outdated if RNi[j] > n.
        SuzukiKasamiMutex.setRN(i, n);

        // If Sj has the idle token, then it sends the token to Si if RNj[i] = LN[i] + 1.

        System.out.println("MY TOKEN: " + SuzukiKasamiMutex.TOKEN);

        if (SuzukiKasamiMutex.hasToken()) {
            System.out.println("IS IN CRITICAL SECTION : " + SuzukiKasamiMutex.isInCS());
            if (!SuzukiKasamiMutex.isInCS()) {
                System.out.println("SuzukiKasamiMutex.RN.get(i) == SuzukiKasamiMutex.getLN(i) + 1 : " + SuzukiKasamiMutex.RN.get(i) + " == " + SuzukiKasamiMutex.getLN(i) + " + " + 1);
                if (SuzukiKasamiMutex.RN.get(i) == SuzukiKasamiMutex.getLN(i) + 1) {
                    ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(i);

                    MessageUtil.sendMessage(new TokenReplyMessage(
                            AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                            nextNode.getIpAddress(), nextNode.getListenerPort(),
                            SuzukiKasamiMutex.TOKEN.copy(), i
                    ));
                    SuzukiKasamiMutex.releaseToken();
                }
            }
        }
        // Since Suzuki-Kasami depends on sending every node a request, if we don't have a token it doesn't matter,
        // we don't have to propagate anything
    }
}
