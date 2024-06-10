package servent.handler.snapshot.naive;

import app.configuration.AppConfig;
import app.snapshot_collector.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class NaiveTellAmountHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public NaiveTellAmountHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.NAIVE_TELL_AMOUNT) {
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }

        int neighborAmount = Integer.parseInt(clientMessage.getMessageText());

        snapshotCollector.addNaiveSnapshotInfo(
                "node" + clientMessage.getOriginalSenderInfo().id(), neighborAmount);
    }

}
