package servent.message.snapshot.li;

import app.ServentInfo;
import app.bitcake_manager.li.LiSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class BlankMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -8565051224822728285L;

    public BlankMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo) {
        super(MessageType.BLANK, originalSenderInfo, receiverInfo);
    }
}
