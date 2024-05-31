package cli.command;

import cli.Command;
import cli.CommandLineInterface;
import matrix_brain.MatrixBrain;
import system_explorer.SystemExplorer;
import task.TaskCoordinator;

public class StopCommand implements Command {

    private final CommandLineInterface commandLineInterface;
    private final TaskCoordinator taskCoordinator;
    private final SystemExplorer systemExplorer;
    private final MatrixBrain matrixBrain;

    public StopCommand(CommandLineInterface commandLineInterface, TaskCoordinator taskCoordinator, SystemExplorer systemExplorer, MatrixBrain matrixBrain) {
        this.commandLineInterface = commandLineInterface;
        this.taskCoordinator = taskCoordinator;
        this.systemExplorer = systemExplorer;
        this.matrixBrain = matrixBrain;
    }

    @Override
    public String commandName() {
        return "stop";
    }

    @Override
    public void execute(String args) {
        matrixBrain.stop();
        systemExplorer.stop();
        taskCoordinator.stop();
        commandLineInterface.stop();
    }
}
