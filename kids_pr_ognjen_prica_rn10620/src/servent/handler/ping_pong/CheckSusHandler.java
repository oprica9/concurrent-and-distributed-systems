package servent.handler.ping_pong;

import app.AppConfig;
import app.ChordState;
import app.failure_detection.FailureDetector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.ping_pong.CheckSusNodeMessage;
import servent.message.ping_pong.UAliveMessage;
import servent.message.util.MessageUtil;

import java.util.ArrayList;
import java.util.List;

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

        CheckSusNodeMessage checkSusNodeMessage = (CheckSusNodeMessage) clientMessage;
        String susIp = checkSusNodeMessage.getSusIp();
        int susPort = checkSusNodeMessage.getSusPort();

        int concernedId = ChordState.chordHash2(clientMessage.getSenderIpAddress(), clientMessage.getSenderPort());

        MessageUtil.sendMessage(new UAliveMessage(
                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                susIp, susPort,
                concernedId
        ));
    }
}
