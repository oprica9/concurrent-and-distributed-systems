package servent.handler.snapshot.ly;

import app.AppConfig;
import app.snapshot_bitcake.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ly.LYTellMessage;

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
            return;
        }

        LYTellMessage lyTellMessage = (LYTellMessage) clientMessage;

        snapshotCollector.addLYSnapshotInfo(
                lyTellMessage.getOriginalSenderInfo().id(),
                lyTellMessage.getLYSnapshotResult());
    }

}
