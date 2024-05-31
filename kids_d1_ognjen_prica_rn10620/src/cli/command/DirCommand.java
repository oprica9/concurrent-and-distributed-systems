package cli.command;

import cli.Command;
import system_explorer.SystemExplorer;

public class DirCommand implements Command {

    private final SystemExplorer systemExplorer;

    public DirCommand(SystemExplorer systemExplorer) {
        this.systemExplorer = systemExplorer;
    }

    @Override
    public String commandName() {
        return "dir";
    }

    @Override
    public void execute(String args) {
        String[] splitArgs = args.split(" ");
        if (splitArgs.length != 1) {
            System.out.println("Invalid arguments for dir. Usage: dir <directory_name>");
            return;
        }

        systemExplorer.addDirectory(args);
    }
}
