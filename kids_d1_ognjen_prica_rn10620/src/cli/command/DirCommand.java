package cli.command;

import cli.Command;
import system_explorer.SystemExplorer;

import java.io.File;

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
        File file = new File(args);
        if (!file.isDirectory()) {
            System.out.println("Directory \"" + args + "\" doesn't exist.");
        }

        systemExplorer.addDirectory(args);
    }
}
