package cli.command;

import cli.Command;
import cli.CommandInfo;

import java.util.List;

public class HelpCommand implements Command {

    private final List<CommandInfo> map;

    public HelpCommand(List<CommandInfo> commandHelp) {
        this.map = commandHelp;
    }

    @Override
    public String commandName() {
        return "help";
    }

    @Override
    public void execute(String args) {
        for (CommandInfo info : map) {
            System.out.printf("%-16s %-32s\n", info.getName(), info.getDescription());
            if (!info.getArguments().isEmpty()) {
                System.out.printf("%-16s %s", " ", "Usage: " + info.getName() + " ");
                for (String arg : info.getArguments()) {
                    System.out.printf("[%s] ", arg);
                }
                System.out.println();
            }
            System.out.println();
        }
    }
}
