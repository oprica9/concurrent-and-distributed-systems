package servent.handler.ping_pong;

import app.configuration.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ping_pong.PongMessage;
import servent.message.util.MessageUtil;

/**
 * Handler for the PING message - sends a PONG back to sender.
 *
 * @author bmilojkovic
 */
public class PingHandler implements MessageHandler {

    private final Message clientMessage;

    public PingHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        //Yap ... it's a PING
        if (clientMessage.getMessageType() == MessageType.PING) {
            /*
             * When we get a PING, we send a PONG back.
             * Notice that this is NOT on the same socket, though,
             * because we want this system to be completely asynchronous.
             */
            MessageUtil.sendMessage(
                    new PongMessage(clientMessage.getReceiverInfo(), clientMessage.getOriginalSenderInfo()));

        } else {
            AppConfig.timestampedErrorPrint("PING handler got: " + clientMessage);
        }
    }

}
