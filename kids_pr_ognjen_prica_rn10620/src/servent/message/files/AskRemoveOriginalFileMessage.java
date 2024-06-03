package servent.message.files;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class AskRemoveOriginalFileMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = 662551018842811949L;

    public AskRemoveOriginalFileMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, String filePath, int ownerId) {
        super(MessageType.ASK_REMOVE_ORIGINAL_FILE, senderIpAddress, senderPort, receiverIpAddress, receiverPort, filePath + "," + ownerId);
    }

}
