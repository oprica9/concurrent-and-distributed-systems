package cli;

public class CommandExecutor {

    public void executeCommand(Command command, String args) {
        command.execute(args);
    }

}
