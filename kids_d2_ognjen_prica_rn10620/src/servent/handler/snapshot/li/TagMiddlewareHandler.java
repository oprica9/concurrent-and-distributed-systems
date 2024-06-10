package servent.handler.snapshot.li;

import app.bitcake_manager.li.LiBitcakeManager;
import app.snapshot_collector.SnapshotCollector;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;

public class TagMiddlewareHandler implements MessageHandler {

    private final Message clientMessage;
    private final SnapshotCollector snapshotCollector;
    private final MessageHandler nextHandler;

    public TagMiddlewareHandler(Message clientMessage, SnapshotCollector snapshotCollector, MessageHandler nextHandler) {
        this.clientMessage = clientMessage;
        this.snapshotCollector = snapshotCollector;
        this.nextHandler = nextHandler;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() == MessageType.LI_MARKER
                || clientMessage.getMessageType() == MessageType.LI_TELL) {
            LiBitcakeManager liBitcakeManager = ((LiBitcakeManager) snapshotCollector.getBitcakeManager());
            liBitcakeManager.markerEvent(clientMessage.getTag(), clientMessage.getOriginalSenderInfo().id(), snapshotCollector);
        }
        nextHandler.run();
    }
}
