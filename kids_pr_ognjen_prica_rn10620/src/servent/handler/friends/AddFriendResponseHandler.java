package servent.handler.friends;

import app.AppConfig;
import app.ChordState;
import app.model.ServentInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.friends.AddFriendResponseMessage;
import servent.message.util.MessageUtil;

public class AddFriendResponseHandler implements MessageHandler {

    private final Message clientMessage;

    public AddFriendResponseHandler(Message clientMessage) {
        this.clientMessage = clientMessage;
    }

    @Override
    public void run() {
        if (clientMessage.getMessageType() != MessageType.ADD_FRIEND_RESPONSE) {
            AppConfig.timestampedErrorPrint("Add friend response handler got a message that is not ADD_FRIEND_RESPONSE");
            return;
        }

        String[] hashRes = clientMessage.getMessageText().split(":");

        int toFindHash;
        try {
            toFindHash = Integer.parseInt(hashRes[0]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Add friend response handler got an invalid friend request response: " + e);
            return;
        }

        String senderIpPort = clientMessage.getSenderIpAddress() + ":" + clientMessage.getSenderPort();

        if (toFindHash == AppConfig.myServentInfo.getChordId()) {
            AppConfig.timestampedStandardPrint(senderIpPort + " accepted your friend request.");
            AppConfig.addFriend(ChordState.chordHash2(clientMessage.getSenderIpAddress(), clientMessage.getSenderPort()));
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
