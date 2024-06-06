package servent.handler.ping_pong;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ping_pong.AmAliveMessage;
import servent.message.ping_pong.UAliveMessage;
import servent.message.util.MessageUtil;

public class UAliveHandler implements MessageHandler {

    private final Message clientMessage;

    public UAliveHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.U_ALIVE) {
            AppConfig.timestampedErrorPrint("You alive handler got a message that is not U_ALIVE");
            return;
        }

        MessageUtil.sendMessage(new AmAliveMessage(
                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                ((UAliveMessage)clientMessage).getConcernedId()
        ));
    }
}
