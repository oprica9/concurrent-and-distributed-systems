package servent.handler.snapshot.chandy_lamport;

import app.bitcake_manager.chandy_lamport.ChandyLamportBitcakeManager;
import app.configuration.AppConfig;
import app.snapshot_collector.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class CLMarkerHandler implements MessageHandler {

    private final Message clientMessage;
    private final ChandyLamportBitcakeManager bitcakeManager;
    private final SnapshotCollector snapshotCollector;

    public CLMarkerHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.bitcakeManager = (ChandyLamportBitcakeManager) snapshotCollector.getBitcakeManager();
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.CL_MARKER) {
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }

        bitcakeManager.handleMarker(clientMessage, snapshotCollector);
    }

}
