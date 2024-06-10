package servent.message.snapshot.li;

import app.ServentInfo;
import app.bitcake_manager.li.LiSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LiTellMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = 3116394054726162318L;
    private final Map<Integer, LiSnapshotResult> liSnapshotResults;
    private final Set<Integer> idBorderSet;

    public LiTellMessage(ServentInfo sender, ServentInfo receiver, Map<Integer, LiSnapshotResult> liSnapshotResults, Set<Integer> idBorderSet) {
        super(MessageType.LI_TELL, sender, receiver);

        this.liSnapshotResults = liSnapshotResults;
        this.idBorderSet = idBorderSet;
    }

    private LiTellMessage(MessageType messageType, ServentInfo sender, ServentInfo receiver,
                          List<ServentInfo> routeList, String messageText, int messageId,
                          Tag tag,
                          Map<Integer, LiSnapshotResult> liSnapshotResults, Set<Integer> idBorderSet) {
        super(messageType, sender, receiver, routeList, messageText, messageId, tag);
        this.liSnapshotResults = liSnapshotResults;
        this.idBorderSet = idBorderSet;
    }

    public Map<Integer, LiSnapshotResult> getLiSnapshotResults() {
        return liSnapshotResults;
    }

    @Override
    public Message setTag(Tag tag) {
        return new LiTellMessage(getMessageType(), getOriginalSenderInfo(), getReceiverInfo(),
                getRoute(), getMessageText(), getMessageId(), tag, getLiSnapshotResults(), getIdBorderSet());
    }

    public Set<Integer> getIdBorderSet() {
        return idBorderSet;
    }
}
