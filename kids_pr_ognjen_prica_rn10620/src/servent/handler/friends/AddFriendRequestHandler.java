package servent.handler.friends;

import app.AppConfig;
import app.ChordState;
import app.model.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.friends.AddFriendRequestMessage;
import servent.message.util.MessageUtil;

public class AddFriendRequestHandler implements MessageHandler {

    private final Message clientMessage;

    public AddFriendRequestHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.ADD_FRIEND_REQUEST) {
            AppConfig.timestampedErrorPrint("Add friend request handler got a message that is not ADD_FRIEND_REQUEST");
            return;
        }

        int toBefriendHash;
        try {
            toBefriendHash = Integer.parseInt(clientMessage.getMessageText());
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Add friend request handler got an invalid friend request: " + e);
            return;
        }

        if (toBefriendHash == AppConfig.myServentInfo.getChordId()) {
            String senderIpPort = clientMessage.getSenderIpAddress() + ":" + clientMessage.getSenderPort();

            AppConfig.timestampedStandardPrint("Received a friend request from " + senderIpPort);

            int requesterHash = ChordState.chordHash2(clientMessage.getSenderIpAddress(), clientMessage.getSenderPort());

            if (!AppConfig.isFriend(requesterHash)) {
                AppConfig.addFriendRequest(requesterHash);
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
