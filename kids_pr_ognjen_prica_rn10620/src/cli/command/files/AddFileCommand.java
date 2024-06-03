package cli.command.files;

import app.AppConfig;
import app.FileManager;
import cli.command.CLICommand;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AddFileCommand implements CLICommand {

    private final static Set<String> ALLOWED_VISIBILITY = new HashSet<>(List.of(new String[]{"private", "public"}));
    private final FileManager fileManager;

    public AddFileCommand(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public String commandName() {
        return "add_file";
    }

    @Override
    public void execute(String args) {
        String[] splitArgs = args.split(" ");
        if (splitArgs.length != 2) {
            AppConfig.timestampedErrorPrint("Invalid arguments for add_file. Usage: add_file <file_path> <private/public>");
            return;
        }

        String filePath = splitArgs[0];
        String visibility = splitArgs[1];

        if (!ALLOWED_VISIBILITY.contains(visibility)) {
            AppConfig.timestampedErrorPrint("Invalid arguments for add_file. Usage: add_file <path> <private/public>");
            return;
        }

        String fileContent = fileManager.readFile(AppConfig.ROOT + "/" + filePath);
        if (fileContent == null) {
            AppConfig.timestampedErrorPrint("Error reading file.");
            return;
        }

        int res = fileManager.addFile(filePath, fileContent, visibility, AppConfig.myServentInfo.getChordId());
        if (res == -2) {
            AppConfig.timestampedStandardPrint("Overwritten file " + filePath + " (" + visibility + ")");
        } else if (res == -1) {
            AppConfig.timestampedStandardPrint("Saved file " + filePath + " (" + visibility + ")");
        }

        fileManager.backupFile(filePath, fileContent, visibility);
    }

}
