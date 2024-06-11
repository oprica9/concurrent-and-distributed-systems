package servent.handler.snapshot.li;

import app.bitcake_manager.li.LiSnapshotResult;
import app.configuration.AppConfig;
import app.snapshot_collector.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.li.ExchangeMessage;

import java.util.Map;

public class ExchangeHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public ExchangeHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.EXCHANGE) {
            AppConfig.timestampedErrorPrint("Exchange handler got: " + clientMessage);
        }

        ExchangeMessage exchangeMessage = (ExchangeMessage) clientMessage;

        Map<Integer, LiSnapshotResult> regionResults = exchangeMessage.getCollectedRegionalValues();
        int regionMasterId = clientMessage.getOriginalSenderInfo().id();

        snapshotCollector.addReceivedRegionResults(regionMasterId, regionResults);
    }

}
