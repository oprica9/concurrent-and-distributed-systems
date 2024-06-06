package cli.command.files;

import app.AppConfig;
import app.ChordState;
import app.FileManager;
import app.model.ServentInfo;
import app.model.StoredFileInfo;
import cli.command.CLICommand;
import servent.message.Message;
import servent.message.files.AskViewFilesMessage;
import servent.message.util.MessageUtil;

import java.util.Map;

public class ViewFilesCommand implements CLICommand {

    private final FileManager fileManager;

    public ViewFilesCommand(FileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public String commandName() {
        return "view_files";
    }

    @Override
    public void execute(String args) {
        if (args == null || args.isEmpty()) {
            // List all files owned by the current node
            Map<String, StoredFileInfo> fileMap = fileManager.getFiles();
            if (fileMap == null || fileMap.isEmpty()) {
                AppConfig.timestampedStandardPrint("No files stored on this node.");
            } else {
                AppConfig.timestampedStandardPrint("Files:");
                FileManager.printFiles(fileMap);
            }
        } else {
            String[] splitArgs = args.split(":");
            if (splitArgs.length != 2) {
                AppConfig.timestampedErrorPrint("Invalid arguments for view_files. Usage: view_files <address:port> e.g. view_files localhost:1100");
                return;
            }

            String ip = splitArgs[0];
            int port;
            try {
                port = Integer.parseInt(splitArgs[1]);
            } catch (NumberFormatException e) {
                AppConfig.timestampedErrorPrint("Invalid arguments for view_files. Usage: view_files <address:port> e.g. view_files localhost:1100");
                return;
            }

            Map<String, StoredFileInfo> fileMap = fileManager.getFilesIfIpPortMatches(ip, port, AppConfig.myServentInfo.getChordId());
            if (fileMap == null) {
                AppConfig.timestampedStandardPrint("Please wait...");
                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(ChordState.chordHash2(ip, port));
                Message askViewFilesMessage = new AskViewFilesMessage(
                        AppConfig.myServentInfo.getIpAddress(),
                        AppConfig.myServentInfo.getListenerPort(),
                        nextNode.getIpAddress(),
                        nextNode.getListenerPort(),
                        ip, port,
                        AppConfig.myServentInfo.getChordId()
                );

                MessageUtil.sendMessage(askViewFilesMessage);
            } else {
                AppConfig.timestampedStandardPrint("Files:");
                FileManager.printFiles(fileMap);
            }
        }
    }
}
