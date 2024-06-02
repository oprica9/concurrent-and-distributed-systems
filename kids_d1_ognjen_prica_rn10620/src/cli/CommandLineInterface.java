package cli;

import app.Cancellable;
import config.AppConfig;
import matrix_brain.MatrixBrain;
import matrix_extractor.MatrixExtractor;
import matrix_multiplier.MatrixMultiplier;
import queue.BlockingTaskQueue;
import queue.TaskQueue;
import system_explorer.SystemExplorer;
import task.TaskCoordinator;

import java.util.Scanner;

public class CommandLineInterface implements Runnable, Cancellable {

    public volatile boolean running;
    private final CommandParser parser;
    private final CommandExecutor executor;
    private final TaskCoordinator taskCoordinator;
    private final SystemExplorer systemExplorer;

    public CommandLineInterface(AppConfig config) {
        TaskQueue taskQueue = new BlockingTaskQueue();
        this.systemExplorer = new SystemExplorer(config.sysExplorerSleepTime(), config.startDir(), taskQueue);

        MatrixBrain matrixBrain = new MatrixBrain();

        MatrixMultiplier matrixMultiplier = new MatrixMultiplier(matrixBrain, config.maximumRowsSize());

        matrixBrain.setMultiplier(matrixMultiplier);
        matrixBrain.setSystemExplorer(systemExplorer);

        this.taskCoordinator = new TaskCoordinator(
                taskQueue,
                new MatrixExtractor(matrixBrain, taskQueue, config.maximumFileChunkSize()),
                matrixMultiplier
        );
        startThreads();

        CommandFactory commandFactory = new CommandFactory(taskQueue, systemExplorer, matrixBrain, this, taskCoordinator);
        this.parser = new CommandParser(commandFactory);
        this.executor = new CommandExecutor();
    }

    private void startThreads() {
        Thread taskCoordinatorThread = new Thread(taskCoordinator, "task_coordinator");
        taskCoordinatorThread.start();

        Thread sysExplorerThread = new Thread(systemExplorer, "sys_explorer");
        sysExplorerThread.start();
    }

    @Override
    public void run() {
        Scanner input = new Scanner(System.in);
        running = true;
        while (running) {
            System.out.print(">");
            String line = input.nextLine();
            if (line.isBlank() || line.isEmpty()) {
                continue;
            }

            int spacePos = line.indexOf(" ");
            String commandName;
            String commandArgs = null;
            if (spacePos != -1) {
                commandName = line.substring(0, spacePos);
                commandArgs = line.substring(spacePos + 1);
            } else {
                commandName = line;
            }

            Command command = parser.parse(commandName);
            if (command != null) {
                try {
                    executor.executeCommand(command, commandArgs);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            } else {
                System.out.println("Unknown command: " + commandName);
            }
        }
        System.out.println("Stopping CommandLineInterface...");
    }

    @Override
    public void stop() {
        running = false;
    }
}
