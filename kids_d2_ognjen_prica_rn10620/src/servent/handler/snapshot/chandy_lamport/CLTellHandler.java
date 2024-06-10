package servent.handler.snapshot.chandy_lamport;

import app.configuration.AppConfig;
import app.snapshot_collector.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.chandy_lamport.CLTellMessage;

public class CLTellHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;

    public CLTellHandler(Message clientMessage, SnapshotCollector snapshotCollector) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.CL_TELL) {
            AppConfig.timestampedErrorPrint("Tell amount handler got: " + clientMessage);
        }

        CLTellMessage clTellMessage = (CLTellMessage) clientMessage;

        snapshotCollector.addCLSnapshotInfo(
                clTellMessage.getOriginalSenderInfo().id(),
                clTellMessage.getCLSnapshotResult());
    }

}
