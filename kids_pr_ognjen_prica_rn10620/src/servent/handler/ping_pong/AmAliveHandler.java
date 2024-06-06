package servent.handler.ping_pong;

import app.AppConfig;
import app.model.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ping_pong.AmAliveMessage;
import servent.message.ping_pong.IsAliveMessage;
import servent.message.util.MessageUtil;

public class AmAliveHandler implements MessageHandler {

    private final Message clientMessage;

    public AmAliveHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.AM_ALIVE) {
            AppConfig.timestampedErrorPrint("Am alive handler got a message that is not AM_ALIVE");
            return;
        }

        int concernedId = ((AmAliveMessage) clientMessage).getConcernedId();

        ServentInfo concernedNode = concernedId == AppConfig.chordState.getPredecessor().getChordId()
                ? AppConfig.chordState.getPredecessor()
                : AppConfig.chordState.getSuccessors()[0];

        MessageUtil.sendMessage(new IsAliveMessage(
                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                concernedNode.getIpAddress(), concernedNode.getListenerPort(),
                clientMessage.getSenderIpAddress(), clientMessage.getSenderPort()
        ));
    }
}
