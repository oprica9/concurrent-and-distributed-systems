package servent.handler.snapshot.li;

import app.configuration.AppConfig;
import app.snapshot_collector.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.li.LiTellMessage;

public class LiTellHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public LiTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.LI_TELL) {
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }

        LiTellMessage liTellMessage = (LiTellMessage) clientMessage;

        System.out.println("Got results from " + clientMessage.getOriginalSenderInfo().id() + ": " + liTellMessage.getLiSnapshotResults());
        System.out.println("Got borderSet from " + clientMessage.getOriginalSenderInfo().id() + ": " + liTellMessage.getIdBorderSet());

        // Update idBorderSet
        AppConfig.idBorderSet.addAll(liTellMessage.getIdBorderSet());

        // Add children results
        snapshotCollector.addLiSnapshotInfos(liTellMessage.getLiSnapshotResults());
    }

}
