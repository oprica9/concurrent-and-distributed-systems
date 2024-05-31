package cli;

public class CommandParser {

    private final CommandFactory commandFactory;

    public CommandParser(CommandFactory commandFactory) {
        this.commandFactory = commandFactory;
    }

    public Command parse(String commandName) {
        return commandFactory.createCommand(commandName);
    }

}
