package servent.handler.dht;

import app.AppConfig;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class PutHandler implements MessageHandler {

    private final Message clientMessage;

    public PutHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.PUT) {
            AppConfig.timestampedErrorPrint("Put handler got a message that is not PUT");
            return;
        }

        String[] splitText = clientMessage.getMessageText().split(":");
        if (splitText.length != 2) {
            AppConfig.timestampedErrorPrint("Got put message with bad text: " + clientMessage.getMessageText());
            return;
        }

        int key;
        int value;

        try {
            key = Integer.parseInt(splitText[0]);
            value = Integer.parseInt(splitText[1]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Got put message with bad text: " + clientMessage.getMessageText());
            return;
        }

        AppConfig.chordState.putValue(key, value);
    }

}
