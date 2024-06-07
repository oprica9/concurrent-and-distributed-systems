package cli;

import app.AppConfig;
import app.Cancellable;
import app.failure_detection.FailureDetector;
import app.file_manager.FileManager;
import app.friend_manager.FriendManager;
import cli.command.*;
import cli.command.dht.DHTGetCommand;
import cli.command.dht.DHTPutCommand;
import cli.command.files.AddFileCommand;
import cli.command.files.OpenFileCommand;
import cli.command.files.RemoveFileCommand;
import cli.command.files.ViewFilesCommand;
import cli.command.friends.AcceptFriendRequestCommand;
import cli.command.friends.AddFriendCommand;
import cli.command.friends.ListFriendRequestsCommand;
import cli.command.friends.ListFriendsCommand;
import servent.SimpleServentListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * A simple CLI parser. Each command has a name and arbitrary arguments.
 * <p>
 * Currently supported commands:
 *
 * <ul>
 * <li><code>info</code> - prints information about the current node</li>
 * <li><code>pause [ms]</code> - pauses execution given number of ms - useful when scripting</li>
 * <li><code>ping [id]</code> - sends a PING message to node [id] </li>
 * <li><code>broadcast [text]</code> - broadcasts the given text to all nodes</li>
 * <li><code>causal_broadcast [text]</code> - causally broadcasts the given text to all nodes</li>
 * <li><code>print_causal</code> - prints all received causal broadcast messages</li>
 * <li><code>stop</code> - stops the servent and program finishes</li>
 * </ul>
 *
 * @author bmilojkovic
 */
public class CLIParser implements Runnable, Cancellable {

    private volatile boolean working = true;
    private final List<CLICommand> commandList;

    public CLIParser(SimpleServentListener listener, FailureDetector failureDetector, FriendManager friendManager, FileManager fileManager) {
        this.commandList = new ArrayList<>();

        commandList.add(new InfoCommand());
        commandList.add(new PauseCommand());
        commandList.add(new SuccessorInfoCommand());
        commandList.add(new DHTGetCommand());
        commandList.add(new DHTPutCommand());
        commandList.add(new StopCommand(this, listener, failureDetector));

        commandList.add(new AddFileCommand(fileManager));
        commandList.add(new ViewFilesCommand(fileManager));
        commandList.add(new RemoveFileCommand(fileManager));
        commandList.add(new OpenFileCommand(fileManager));

        commandList.add(new AddFriendCommand(friendManager));
        commandList.add(new AcceptFriendRequestCommand(friendManager));
        commandList.add(new ListFriendsCommand(friendManager));
        commandList.add(new ListFriendRequestsCommand(friendManager));
    }

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);

        while (working) {
            String commandLine = sc.nextLine();

            int spacePos = commandLine.indexOf(" ");

            String commandName;
            String commandArgs = null;
            if (spacePos != -1) {
                commandName = commandLine.substring(0, spacePos);
                commandArgs = commandLine.substring(spacePos + 1);
            } else {
                commandName = commandLine;
            }

            boolean found = false;

            for (CLICommand cliCommand : commandList) {
                if (cliCommand.commandName().equals(commandName)) {
                    cliCommand.execute(commandArgs);
                    found = true;
                    break;
                }
            }

            if (!found) {
                AppConfig.timestampedErrorPrint("Unknown command: " + commandName);
            }
        }

        sc.close();
    }

    @Override
    public void stop() {
        this.working = false;
    }
}
