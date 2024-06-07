package servent.handler.friends;

import app.AppConfig;
import app.ChordState;
import app.friend_manager.FriendManager;
import app.model.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.friends.AddFriendRequestMessage;
import servent.message.util.MessageUtil;

public class AddFriendRequestHandler implements MessageHandler {

    private final Message clientMessage;
    private final FriendManager friendManager;

    public AddFriendRequestHandler(Message clientMessage, FriendManager friendManager) {
        this.clientMessage = clientMessage;
        this.friendManager = friendManager;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.ADD_FRIEND_REQUEST) {
            AppConfig.timestampedErrorPrint("Add friend request handler got a message that is not ADD_FRIEND_REQUEST");
            return;
        }

        AddFriendRequestMessage addFriendRequestMessage = (AddFriendRequestMessage) clientMessage;

        int toBefriendHash = addFriendRequestMessage.getToBefriendHash();

        if (toBefriendHash == AppConfig.myServentInfo.getChordId()) {
            String senderIpPort = clientMessage.getSenderIpAddress() + ":" + clientMessage.getSenderPort();

            AppConfig.timestampedStandardPrint("Received a friend request from " + senderIpPort);

            int requesterHash = ChordState.chordHash2(clientMessage.getSenderIpAddress(), clientMessage.getSenderPort());

            if (!friendManager.isFriend(requesterHash)) {
                friendManager.addFriendRequest(requesterHash, clientMessage.getSenderIpAddress(), clientMessage.getSenderPort());
            }
        } else {
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(toBefriendHash);

            Message message = new AddFriendRequestMessage(
                    clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                    nextNode.getIpAddress(), nextNode.getListenerPort(),
                    toBefriendHash
            );
            MessageUtil.sendMessage(message);
        }

    }
}
