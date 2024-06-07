package cli.command.friends;

import app.AppConfig;
import app.ChordState;
import app.friend_manager.FriendManager;
import app.model.ServentInfo;
import cli.command.CLICommand;
import servent.message.Message;
import servent.message.friends.AddFriendResponseMessage;
import servent.message.util.MessageUtil;

public class AcceptFriendRequestCommand implements CLICommand {

    private final FriendManager friendManager;

    public AcceptFriendRequestCommand(FriendManager friendManager) {
        this.friendManager = friendManager;
    }

    @Override
    public String commandName() {
        return "accept_friend";
    }

    @Override
    public void execute(String args) {
        if (args == null || args.isEmpty()) {
            AppConfig.timestampedErrorPrint("Invalid arguments for accept_friend. Usage: accept_friend <address:port> e.g. accept_friend localhost:1100");
            return;
        }

        String[] splitArgs = args.split(":");
        if (splitArgs.length != 2) {
            AppConfig.timestampedErrorPrint("Invalid arguments for accept_friend. Usage: accept_friend <address:port> e.g. accept_friend localhost:1100");
            return;
        }

        String ip = splitArgs[0];
        int port;
        try {
            port = Integer.parseInt(splitArgs[1]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Invalid arguments for accept_friend. Usage: accept_friend <address:port> e.g. accept_friend localhost:1100");
            return;
        }

        int toAccept = ChordState.chordHash2(ip, port);
        if (friendManager.haveRequest(toAccept)) {
            // You're my friend now, we're having soft tacos later!
            AppConfig.timestampedStandardPrint("Accepted friend request from " + ip + ":" + port);
            friendManager.addFriend(toAccept, ip, port);

            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(toAccept);

            Message responseMessage = new AddFriendResponseMessage(
                    AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                    nextNode.getIpAddress(), nextNode.getListenerPort(),
                    ChordState.chordHash2(ip, port)
            );
            MessageUtil.sendMessage(responseMessage);

        } else {
            AppConfig.timestampedStandardPrint("There is no friend request from " + ip + ":" + port);
        }
    }
}
