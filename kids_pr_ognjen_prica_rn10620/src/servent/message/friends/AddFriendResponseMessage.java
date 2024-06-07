package servent.message.friends;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class AddFriendResponseMessage extends BasicMessage {
    @Serial
    private static final long serialVersionUID = -174982751601187295L;
    private final int requesterHash;

    public AddFriendResponseMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, int requesterHash) {
        super(MessageType.ADD_FRIEND_RESPONSE, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.requesterHash = requesterHash;
    }

    public int getRequesterHash() {
        return requesterHash;
    }
}
