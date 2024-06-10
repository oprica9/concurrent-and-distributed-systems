package servent.message.snapshot.li;

import app.ServentInfo;
import app.bitcake_manager.li.LiSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class ExchangeMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 6720727401653217251L;
    private final Map<Integer, LiSnapshotResult> collectedRegionalValues;

    public ExchangeMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, Map<Integer, LiSnapshotResult> collectedRegionalValues) {
        super(MessageType.EXCHANGE, originalSenderInfo, receiverInfo);
        this.collectedRegionalValues = collectedRegionalValues;
    }

    private ExchangeMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver, List<ServentInfo> routeList, String messageText,
                            int messageId, Tag tag, Map<Integer, LiSnapshotResult> collectedRegionalValues) {
        super(messageType, sender, receiver, routeList, messageText, messageId, tag);
        this.collectedRegionalValues = collectedRegionalValues;
    }

    public Map<Integer, LiSnapshotResult> getCollectedRegionalValues() {
        return collectedRegionalValues;
    }

    @Override
    public Message setTag(Tag tag) {
        return new ExchangeMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
                getRoute(), getMessageText(), getMessageId(), tag, getCollectedRegionalValues());
    }
}
