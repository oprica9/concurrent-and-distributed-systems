package servent.handler.snapshot.li;

import app.bitcake_manager.li.LiSnapshotResult;
import app.configuration.AppConfig;
import app.snapshot_collector.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.li.ExchangeMessage;

import java.util.Map;

public class BlankHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public BlankHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.BLANK) {
            AppConfig.timestampedErrorPrint("Blank handler got: " + clientMessage);
        }

        snapshotCollector.addReceivedBlank(clientMessage.getOriginalSenderInfo().id());
    }

}
