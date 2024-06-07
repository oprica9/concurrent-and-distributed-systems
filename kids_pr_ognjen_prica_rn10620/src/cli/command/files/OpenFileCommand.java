package cli.command.files;

import app.AppConfig;
import app.file_manager.FileManager;
import app.file_manager.FileOpener;
import app.model.FileInfo;
import app.model.FileType;
import app.model.ImageFileInfo;
import app.model.TextFileInfo;
import cli.command.CLICommand;

public class OpenFileCommand implements CLICommand {

    private final FileManager fileManager;

    public OpenFileCommand(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public String commandName() {
        return "open_file";
    }

    @Override
    public void execute(String args) {
        if (fileManager.containsFile(args)) {
            FileInfo info = fileManager.getFile(args);
            if (info.getType() == FileType.TEXT) {
                FileOpener.openTextFile((TextFileInfo) info);
            } else {
                FileOpener.openImageFile((ImageFileInfo) info);
            }
        } else {
            AppConfig.timestampedErrorPrint("No such file: " + args);
        }
    }
}
