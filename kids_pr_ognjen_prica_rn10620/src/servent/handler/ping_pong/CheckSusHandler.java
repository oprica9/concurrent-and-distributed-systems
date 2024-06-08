package servent.handler.ping_pong;

import app.AppConfig;
import app.ChordState;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ping_pong.CheckSusMessage;
import servent.message.ping_pong.UAliveMessage;
import servent.message.util.MessageUtil;

public class CheckSusHandler implements MessageHandler {

    private final Message clientMessage;

    public CheckSusHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.CHECK_SUS) {
            AppConfig.timestampedErrorPrint("Check suspicious node handler got a message that is not CHECK_SUS");
            return;
        }

        CheckSusMessage checkSusMessage = (CheckSusMessage) clientMessage;
        String susIp = checkSusMessage.getSusIp();
        int susPort = checkSusMessage.getSusPort();

        int concernedId = ChordState.chordHash2(clientMessage.getSenderIpAddress(), clientMessage.getSenderPort());

        MessageUtil.sendMessage(new UAliveMessage(
                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                susIp, susPort,
                concernedId
        ));
    }
}
