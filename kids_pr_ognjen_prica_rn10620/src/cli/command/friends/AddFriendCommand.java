package cli.command.friends;

import app.AppConfig;
import app.ChordState;
import app.friend_manager.FriendManager;
import app.model.ServentInfo;
import cli.command.CLICommand;
import servent.message.Message;
import servent.message.friends.AddFriendRequestMessage;
import servent.message.util.MessageUtil;

public class AddFriendCommand implements CLICommand {

    private final FriendManager friendManager;

    public AddFriendCommand(FriendManager friendManager) {
        this.friendManager = friendManager;
    }

    @Override
    public String commandName() {
        return "add_friend";
    }

    @Override
    public void execute(String args) {
        if (args == null || args.isEmpty()) {
            AppConfig.timestampedErrorPrint("Invalid arguments for add_friend. Usage: add_friend <address:port> e.g. add_friend localhost:1100");
            return;
        }

        String[] splitArgs = args.split(":");
        if (splitArgs.length != 2) {
            AppConfig.timestampedErrorPrint("Invalid arguments for add_friend. Usage: add_friend <address:port> e.g. add_friend localhost:1100");
            return;
        }

        String ip = splitArgs[0];
        int port;
        try {
            port = Integer.parseInt(splitArgs[1]);
        } catch (NumberFormatException e) {
            AppConfig.timestampedErrorPrint("Invalid arguments for add_friend. Usage: add_friend <address:port> e.g. add_friend localhost:1100");
            return;
        }

        if (ip.equals(AppConfig.myServentInfo.getIpAddress()) && port == AppConfig.myServentInfo.getListenerPort()) {
            AppConfig.timestampedErrorPrint("Can't send a friend request to yourself silly!");
            return;
        }

        int toBefriendHash = ChordState.chordHash2(ip, port);

        if (friendManager.isFriend(toBefriendHash)) {
            AppConfig.timestampedStandardPrint(ip + ":" + port + " is already your friend!");
            return;
        }

        ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(toBefriendHash);

        Message message = new AddFriendRequestMessage(
                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                nextNode.getIpAddress(), nextNode.getListenerPort(),
                toBefriendHash
        );
        MessageUtil.sendMessage(message);

    }
}
