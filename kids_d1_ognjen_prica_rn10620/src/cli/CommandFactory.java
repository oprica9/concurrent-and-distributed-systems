package cli;

import cli.command.*;
import matrix_brain.MatrixBrain;
import queue.TaskQueue;
import system_explorer.SystemExplorer;
import task.TaskCoordinator;

import java.util.ArrayList;
import java.util.List;

public class CommandFactory {

    private static final String DIR = "dir";
    private static final String INFO = "info";
    private static final String MULTIPLY = "multiply";
    private static final String SAVE = "save";
    private static final String CLEAR = "clear";
    private static final String STOP = "stop";
    private static final String HELP = "help";
    private final List<CommandInfo> commandHelpList;
    private final List<Command> commandList;

    private final TaskQueue taskQueue;
    private final SystemExplorer systemExplorer;
    private final MatrixBrain matrixBrain;
    private final CommandLineInterface commandLineInterface;
    private final TaskCoordinator taskCoordinator;

    public CommandFactory(TaskQueue taskQueue, SystemExplorer systemExplorer, MatrixBrain matrixBrain, CommandLineInterface commandLineInterface, TaskCoordinator taskCoordinator) {
        this.taskQueue = taskQueue;
        this.systemExplorer = systemExplorer;
        this.matrixBrain = matrixBrain;
        this.commandLineInterface = commandLineInterface;
        this.taskCoordinator = taskCoordinator;

        this.commandList = new ArrayList<>();
        initCommandList();

        this.commandHelpList = new ArrayList<>();
        initHelp();
    }

    public Command createCommand(String commandName) {
        for (Command command : commandList) {
            if (command.commandName().equals(commandName)) {
                return command;
            }
        }
        return null;
    }

    private void initCommandList() {
        commandList.add(new DirCommand(systemExplorer));
        commandList.add(new InfoCommand(matrixBrain));
        commandList.add(new MultiplyCommand(taskQueue, matrixBrain));
        commandList.add(new SaveCommand(matrixBrain));
        commandList.add(new ClearCommand(matrixBrain));
        commandList.add(new StopCommand(commandLineInterface, taskCoordinator, systemExplorer, matrixBrain));
        commandList.add(new HelpCommand(commandHelpList));
    }

    private void initHelp() {
        CommandInfo dirCommand = new CommandInfo(DIR, "Adds a new directory for scanning matrices.", "<directory_name>");
        CommandInfo infoCommand = new CommandInfo(INFO, "Fetches basic information about a specific matrix or a set of matrices.", "<matrix_name>", "-all", "-asc | -desc", "-s | -e <number>");
        CommandInfo multiplyCommand = new CommandInfo(MULTIPLY, "Multiplies two matrices.", "<matrix1_name,matrix2_name>", "-name <product_name>");
        CommandInfo saveCommand = new CommandInfo(SAVE, "Saves a matrix to disk.", "-name <matrix_name>", "-file <file_name>");
        CommandInfo clearCommand = new CommandInfo(CLEAR, "Rescans the requested matrix from file.", "<matrix_name | file_name>");
        CommandInfo stopCommand = new CommandInfo(STOP, "Shuts down the application.");
        CommandInfo helpCommand = new CommandInfo(HELP, "Lists out a manual how to use the supported commands.");
        commandHelpList.add(dirCommand);
        commandHelpList.add(infoCommand);
        commandHelpList.add(multiplyCommand);
        commandHelpList.add(saveCommand);
        commandHelpList.add(clearCommand);
        commandHelpList.add(stopCommand);
        commandHelpList.add(helpCommand);
    }

}
