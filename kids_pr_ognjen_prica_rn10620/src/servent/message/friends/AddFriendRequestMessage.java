package servent.message.friends;

import servent.message.BasicMessage;
import servent.message.MessageType;

import java.io.Serial;

public class AddFriendRequestMessage extends BasicMessage {

    @Serial
    private static final long serialVersionUID = -7261732789285637489L;
    private final int toBefriendHash;

    public AddFriendRequestMessage(String senderIpAddress, int senderPort, String receiverIpAddress, int receiverPort, int toBefriendHash) {
        super(MessageType.ADD_FRIEND_REQUEST, senderIpAddress, senderPort, receiverIpAddress, receiverPort);
        this.toBefriendHash = toBefriendHash;
    }

    public int getToBefriendHash() {
        return toBefriendHash;
    }
}
