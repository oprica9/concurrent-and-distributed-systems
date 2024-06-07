package servent.handler.friends;

import app.AppConfig;
import app.ChordState;
import app.friend_manager.FriendManager;
import app.model.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.friends.AddFriendResponseMessage;
import servent.message.util.MessageUtil;

public class AddFriendResponseHandler implements MessageHandler {

    private final Message clientMessage;
    private final FriendManager friendManager;

    public AddFriendResponseHandler(Message clientMessage, FriendManager friendManager) {
        this.clientMessage = clientMessage;
        this.friendManager = friendManager;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.ADD_FRIEND_RESPONSE) {
            AppConfig.timestampedErrorPrint("Add friend response handler got a message that is not ADD_FRIEND_RESPONSE");
            return;
        }

        AddFriendResponseMessage addFriendResponseMessage = (AddFriendResponseMessage) clientMessage;

        int toFindHash = addFriendResponseMessage.getRequesterHash();

        String ip = clientMessage.getSenderIpAddress();
        int port = clientMessage.getSenderPort();
        String senderAddress = friendManager.getAddress(ip, port);

        if (toFindHash == AppConfig.myServentInfo.getChordId()) {
            AppConfig.timestampedStandardPrint(senderAddress + " accepted your friend request.");
            friendManager.addFriend(ChordState.chordHash2(ip, port), ip, port);
        } else {
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(toFindHash);

            Message responseMessage = new AddFriendResponseMessage(
                    clientMessage.getSenderIpAddress(), clientMessage.getSenderPort(),
                    nextNode.getIpAddress(), nextNode.getListenerPort(),
                    toFindHash
            );
            MessageUtil.sendMessage(responseMessage);
        }
    }
}
