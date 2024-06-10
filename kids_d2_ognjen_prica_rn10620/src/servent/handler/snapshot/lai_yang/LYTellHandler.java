package servent.handler.snapshot.lai_yang;

import app.configuration.AppConfig;
import app.snapshot_collector.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.lai_yang.LYTellMessage;

public class LYTellHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public LYTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.LY_TELL) {
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }

        LYTellMessage lyTellMessage = (LYTellMessage) clientMessage;

        snapshotCollector.addLYSnapshotInfo(
                lyTellMessage.getOriginalSenderInfo().id(),
                lyTellMessage.getLYSnapshotResult());
    }

}
