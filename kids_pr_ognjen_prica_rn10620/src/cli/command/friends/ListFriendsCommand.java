package cli.command.friends;

import app.friend_manager.FriendManager;
import cli.command.CLICommand;

public class ListFriendsCommand implements CLICommand {

    private final FriendManager friendManager;

    public ListFriendsCommand(FriendManager friendManager) {
        this.friendManager = friendManager;
    }

    @Override
    public String commandName() {
        return "list_friends";
    }

    @Override
    public void execute(String args) {
        friendManager.printFriends();
    }
}
