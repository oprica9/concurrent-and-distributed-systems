package cli.command.friends;

import app.AppConfig;
import cli.command.CLICommand;

public class ListFriendsCommand implements CLICommand {
    @Override
    public String commandName() {
        return "list_friends";
    }

    @Override
    public void execute(String args) {
        AppConfig.printFriends();
    }
}
