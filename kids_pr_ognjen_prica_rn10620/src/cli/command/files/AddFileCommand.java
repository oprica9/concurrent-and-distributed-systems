package cli.command.files;

import app.AppConfig;
import app.file_manager.FileManager;
import app.model.FileInfo;
import app.model.FileType;
import app.model.ImageFileInfo;
import app.model.TextFileInfo;
import cli.command.CLICommand;

import java.awt.image.BufferedImage;
import java.io.IOException;

public class AddFileCommand implements CLICommand {

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

        if (!FileManager.ALLOWED_VISIBILITY.contains(visibility)) {
            AppConfig.timestampedErrorPrint("Invalid arguments for add_file. Usage: add_file <path> <private/public>");
            return;
        }

        FileType type;
        try {
            type = fileManager.getFileType(filePath);
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Unsupported file extension " + filePath);
            return;
        }

        FileInfo fileInfo;
        try {
            if (type == FileType.TEXT) {
                fileInfo = new TextFileInfo(
                        filePath,
                        fileManager.getExtension(filePath),
                        (String) fileManager.readFile(filePath),
                        visibility,
                        AppConfig.myServentInfo.getChordId(),
                        0);
            } else {
                fileInfo = new ImageFileInfo(
                        filePath,
                        fileManager.getExtension(filePath),
                        (BufferedImage) fileManager.readFile(filePath),
                        visibility,
                        AppConfig.myServentInfo.getChordId(),
                        0
                );
            }
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Error reading file content: " + filePath + ". Reason: " + e.getMessage());
            return;
        }

        int res = fileManager.addFile(fileInfo);
        if (res == -2) {
            AppConfig.timestampedStandardPrint("Overwritten file " + filePath + " (" + visibility + ")");
        } else if (res == -1) {
            AppConfig.timestampedStandardPrint("Saved file " + filePath + " (" + visibility + ")");
        }

        // Backup only if the file is public
        if (fileManager.getFileVisibility(filePath).equals(FileManager.PUBLIC)) {
            try {
                fileManager.backupFile(fileInfo);
            } catch (IOException e) {
                AppConfig.timestampedErrorPrint("Unable to backup file: " + filePath + ". Reason: " + e.getMessage());
            }
        }
    }

}
