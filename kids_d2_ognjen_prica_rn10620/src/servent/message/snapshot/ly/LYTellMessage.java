package servent.message.snapshot.ly;

import java.io.Serial;
import java.util.List;

import app.ServentInfo;
import app.snapshot_bitcake.ly.LYSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class LYTellMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 3116394054726162318L;

    private final LYSnapshotResult lySnapshotResult;

    public LYTellMessage(ServentInfo sender, ServentInfo receiver, LYSnapshotResult lySnapshotResult) {
        super(MessageType.LY_TELL, sender, receiver);

        this.lySnapshotResult = lySnapshotResult;
    }

    private LYTellMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver,
                          boolean white, List<ServentInfo> routeList, String messageText, int messageId,
                          LYSnapshotResult lySnapshotResult) {
        super(messageType, sender, receiver, white, routeList, messageText, messageId);
        this.lySnapshotResult = lySnapshotResult;
    }

    public LYSnapshotResult getLYSnapshotResult() {
        return lySnapshotResult;
    }

    @Override
    public Message setRedColor() {
        return new LYTellMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
                false, getRoute(), getMessageText(), getMessageId(), getLYSnapshotResult());
    }
}
