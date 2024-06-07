package cli.command.files;

import app.AppConfig;
import app.file_manager.FileManager;
import cli.command.CLICommand;

import java.io.IOException;

public class RemoveFileCommand implements CLICommand {

    private final FileManager fileManager;

    public RemoveFileCommand(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public String commandName() {
        return "remove_file";
    }

    @Override
    public void execute(String args) {
        if (args.isEmpty() || args.isBlank()) {
            AppConfig.timestampedErrorPrint("Invalid arguments for remove_file. Usage: remove_file <file_name>");
            return;
        }

        try {
            fileManager.removeFile(args);
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Unable to remove file: " + args + ".Reason: " + e.getMessage());
        }

        AppConfig.timestampedStandardPrint("Please wait...");
    }
}
