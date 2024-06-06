package app;

import app.model.ServentInfo;
import app.model.StoredFileInfo;
import servent.message.WelcomeMessage;
import servent.message.files.AskRemoveFileMessage;
import servent.message.files.AskRemoveOriginalFileMessage;
import servent.message.files.BackupFileMessage;
import servent.message.util.MessageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FileManager {

    public static final String PRIVATE = "private";
    public static final String PUBLIC = "public";
    public static final Set<String> ALLOWED_VISIBILITY = new HashSet<>(List.of(new String[]{PRIVATE, PUBLIC}));
    public static final int REPLICATION_FACTOR = 3;
    private Map<String, StoredFileInfo> fileMap = new ConcurrentHashMap<>();

    /**
     * Creates a backup file in the chord system.
     */
    public void backupFile(String filePath, String fileContent, String visibility) {
        // Let's find the node responsible
        int fileHash = fileHash(filePath);

        ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(fileHash);

        StoredFileInfo backupInfo = new StoredFileInfo(filePath, fileContent, visibility, AppConfig.myServentInfo.getChordId(), 0);

        MessageUtil.sendMessage(new BackupFileMessage(
                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                nextNode.getIpAddress(), nextNode.getListenerPort(),
                backupInfo, fileHash
        ));
    }

    /**
     * Saves a file (locally) in the process that calls this command.
     */
    public int addFile(String filePath, String fileContent, String visibility, int ownerId) {
        if (fileMap.containsKey(filePath)) {
            fileMap.put(filePath, new StoredFileInfo(filePath, fileContent, visibility, ownerId));
            return -2;
        } else {
            fileMap.put(filePath, new StoredFileInfo(filePath, fileContent, visibility, ownerId));
            return -1;
        }
    }

    public void addFile(StoredFileInfo backupFile) {
        if (fileMap.containsKey(backupFile.getPath())) {
            AppConfig.timestampedStandardPrint("File already present. Overwriting...");
        }
        fileMap.put(backupFile.getPath(), backupFile);
    }

    public void removeFile(String filePath) {
        AppConfig.timestampedStandardPrint("Attempting to remove file " + filePath);

        int fileHash = FileManager.fileHash(filePath);
        StoredFileInfo toRemove = fileMap.get(filePath);

        if (toRemove != null) {
            // To remove original file
            int ownerId = toRemove.getOwnerKey();
            if (ownerId != AppConfig.myServentInfo.getChordId()) {
                ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(ownerId);

                MessageUtil.sendMessage(new AskRemoveOriginalFileMessage(
                        AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                        nextNode.getIpAddress(), nextNode.getListenerPort(),
                        toRemove.getPath(), ownerId
                ));
            }
        }

        // Find the first backup
        if (AppConfig.chordState.isKeyMine(fileHash)) {
            // I'm the first backup
            fileMap.remove(filePath);

            // Delete the next backups
            MessageUtil.sendMessage(new AskRemoveFileMessage(
                    AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort(),
                    filePath, fileHash,
                    -1));

        } else {
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(fileHash);

            MessageUtil.sendMessage(new AskRemoveFileMessage(
                    AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                    nextNode.getIpAddress(), nextNode.getListenerPort(),
                    filePath, fileHash,
                    1));
        }
    }

    public int deleteBackup(String filePath) {
        StoredFileInfo deleted = fileMap.remove(filePath);

        if (deleted != null) {
            return deleted.getBackupId();
        }

        return -1;
    }

    public String readFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                AppConfig.timestampedErrorPrint("File not found: " + filePath);
                return null;
            }

            return Files.readString(file.toPath());
        } catch (IOException e) {
            AppConfig.timestampedErrorPrint("Error reading file: " + filePath);
            return null;
        }
    }

    public static int fileHash(String filePath) {
        try (FileInputStream fis = new FileInputStream(AppConfig.ROOT + "/" + filePath)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            BigInteger hashValue = new BigInteger(1, hashBytes);

            return hashValue.mod(BigInteger.valueOf(ChordState.CHORD_SIZE)).intValue();
        } catch (Exception e) {
            AppConfig.timestampedErrorPrint("Could not compute hash for file: " + filePath + " due to " + e);
            return -1;
        }
    }

    /**
     * @return file map if the ip and port matches, null otherwise
     */
    public Map<String, StoredFileInfo> getFilesIfIpPortMatches(String ip, int port, int requesterId) {
        return sameIpAndPort(ip, port)
                ? AppConfig.isFriend(requesterId)
                ? fileMap
                : fileMap.entrySet().stream().collect(Collectors.filtering(
                entry -> entry.getValue().getVisibility().equals(PUBLIC),
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                : null;
    }

    public Map<String, StoredFileInfo> getFiles() {
        return fileMap;
    }

    public static void printFiles(Map<String, StoredFileInfo> fileMap) {
        for (Map.Entry<String, StoredFileInfo> entry : fileMap.entrySet()) {
            String filePath = entry.getKey();
            StoredFileInfo fileInfo = entry.getValue();
            AppConfig.timestampedStandardPrint("- " + filePath + " (" + fileInfo.getVisibility() + ")");
        }
    }

    public int removeOriginalFile(String filePath) {
        return fileMap.remove(filePath) != null ? -2 : 0;
    }

    private boolean sameIpAndPort(String ip, int port) {
        return AppConfig.myServentInfo.getIpAddress().equals(ip) && AppConfig.myServentInfo.getListenerPort() == port;
    }

    public void setFiles(Map<String, StoredFileInfo> myFiles) {
        this.fileMap = myFiles;
    }

    public void init(WelcomeMessage welcomeMsg) {
        this.fileMap = welcomeMsg.getFiles();
//        AppConfig.timestampedStandardPrint("My fileMap that I got from my successor:\n" + fileMap);
    }

    public boolean containsFile(StoredFileInfo storedFileInfo) {
        return fileMap.containsKey(storedFileInfo.getPath());
    }

    public boolean containsFile(String filePath) {
        return fileMap.containsKey(filePath);
    }

    public String getFileVisibility(String filePath) {
        return fileMap.containsKey(filePath)
                ? fileMap.get(filePath).getVisibility()
                : "";
    }

    public StoredFileInfo getFile(String filePath) {
        return fileMap.get(filePath);
    }
}
