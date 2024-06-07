package servent.handler.dht;

import app.AppConfig;
import app.model.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.dht.AskGetMessage;
import servent.message.dht.TellGetMessage;
import servent.message.util.MessageUtil;

import java.util.Map;

public class AskGetHandler implements MessageHandler {

    private final Message clientMessage;

    public AskGetHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.ASK_GET) {
            AppConfig.timestampedErrorPrint("Ask get handler got a message that is not ASK_GET");
            return;
        }

        int key;
        try {
            key = Integer.parseInt(clientMessage.getMessageText());
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Got ask get with bad text: " + clientMessage.getMessageText());
            return;
        }

        // Check if the key belongs to the current node
        if (AppConfig.chordState.isKeyMine(key)) {
            TellGetMessage tgm = getTellGetMessage(key);
            MessageUtil.sendMessage(tgm);
        } else {
            // If key is not mine, forward the request to the next node
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(key);

            AskGetMessage agm = new AskGetMessage(
                    clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                    nextNode.getIpAddress(), nextNode.getListenerPort(),
                    clientMessage.getMessageText()
            );
            MessageUtil.sendMessage(agm);
        }
    }

    private TellGetMessage getTellGetMessage(int key) {
        // If key is mine, get the value from the value map
        Map<Integer, Integer> valueMap = AppConfig.chordState.getValueMap();
        int value = -1;

        if (valueMap.containsKey(key)) {
            value = valueMap.get(key);
        }

        return new TellGetMessage(
                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                key, value
        );
    }

}