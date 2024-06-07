package app.file_manager;

import app.AppConfig;
import app.ChordState;
import app.friend_manager.FriendManager;
import app.model.*;
import servent.message.WelcomeMessage;
import servent.message.files.AskRemoveFileMessage;
import servent.message.files.AskRemoveOriginalFileMessage;
import servent.message.files.BackupFileMessage;
import servent.message.util.MessageUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileManager {

    public static final String PRIVATE = "private";
    public static final String PUBLIC = "public";
    public static final Set<String> ALLOWED_VISIBILITY = new HashSet<>(List.of(new String[]{PRIVATE, PUBLIC}));
    public static final Set<String> SUPPORTED_IMG_EXTENSIONS = new HashSet<>(List.of(new String[]{"jpg", "jpeg", "png", "gif", "bmp"}));
    public static final Set<String> SUPPORTED_FILE_EXTENSIONS = new HashSet<>(List.of(new String[]{"txt"}));
    public static final int REPLICATION_FACTOR = 3;
    private final FriendManager friendManager;
    private Map<String, FileInfo> fileMap = new ConcurrentHashMap<>();

    public FileManager(FriendManager friendManager) {
        this.friendManager = friendManager;
    }

    /**
     * Creates a backup file in the chord system.
     */
    public void backupFile(FileInfo infoToBackup) throws IOException {
        int fileHash = fileHash(infoToBackup.getPath());

        System.out.println("File hash for " + infoToBackup.getPath() + ": " + fileHash);

        FileInfo backupInfo = infoToBackup;
        if (AppConfig.chordState.isKeyMine(fileHash)) {
            backupInfo = getInfoWithIncrementedBackupId(infoToBackup);
        }

        ServentInfo nextNode = AppConfig.chordState.isKeyMine(fileHash)
                ? AppConfig.chordState.getNextNode()
                : AppConfig.chordState.getNextNodeForKey(fileHash);

        MessageUtil.sendMessage(new BackupFileMessage(
                AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                nextNode.getIpAddress(), nextNode.getListenerPort(),
                backupInfo, fileHash
        ));
    }

    /**
     * Saves a file (locally) in the process that calls this command.
     */
    public int addFile(FileInfo fileInfo) {
        if (fileMap.containsKey(fileInfo.getPath())) {
            fileMap.put(fileInfo.getPath(), fileInfo);
            return -2;
        } else {
            fileMap.put(fileInfo.getPath(), fileInfo);
            return -1;
        }
    }

    public void removeFile(String filePath) throws IOException {
        AppConfig.timestampedStandardPrint("Attempting to remove file " + filePath);

        int fileHash;
        fileHash = fileHash(filePath);

        // Find the first backup
        if (AppConfig.chordState.isKeyMine(fileHash)) {
            // I'm the first backup
            FileInfo removed = fileMap.remove(filePath);
            if (removed.getOwnerKey() == AppConfig.myServentInfo.getChordId()) {
                AppConfig.timestampedStandardPrint("Removing original file " + filePath + ", backupId: " + removed.getBackupId());
            } else {
                AppConfig.timestampedStandardPrint("Removing backup for file " + filePath + ", backupId: " + removed.getBackupId());
            }


            // Ask for removal of the original if I'm not the original owner
            if (AppConfig.myServentInfo.getChordId() != removed.getOwnerKey()) {
                List<Integer> visited = new ArrayList<>();
                visited.add(AppConfig.myServentInfo.getChordId());

                MessageUtil.sendMessage(new AskRemoveOriginalFileMessage(
                        AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                        AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort(),
                        filePath, removed.getOwnerKey(), visited)
                );
            }

            // Delete the next backups
            MessageUtil.sendMessage(new AskRemoveFileMessage(
                    AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                    AppConfig.chordState.getNextNodeIpAddress(), AppConfig.chordState.getNextNodePort(),
                    filePath, fileHash,
                    1, removed.getOwnerKey()));

        } else {
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(fileHash);

            MessageUtil.sendMessage(new AskRemoveFileMessage(
                    AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                    nextNode.getIpAddress(), nextNode.getListenerPort(),
                    filePath, fileHash,
                    -1, -1));
        }
    }

    public void askRemoveOriginalFile(FileInfo toRemove) {
        int ownerId = toRemove.getOwnerKey();
        if (ownerId != AppConfig.myServentInfo.getChordId()) {
            ServentInfo nextNode = AppConfig.chordState.getNextNodeForKey(ownerId);

            List<Integer> visited = new ArrayList<>();
            visited.add(AppConfig.myServentInfo.getChordId());

            MessageUtil.sendMessage(new AskRemoveOriginalFileMessage(
                    AppConfig.myServentInfo.getIpAddress(), AppConfig.myServentInfo.getListenerPort(),
                    nextNode.getIpAddress(), nextNode.getListenerPort(),
                    toRemove.getPath(), ownerId, visited
            ));
        }
    }

    public int deleteBackup(String filePath) {
        FileInfo deleted = fileMap.remove(filePath);

        if (deleted != null) {
            return deleted.getBackupId();
        }

        return -1;
    }

    public Object readFile(String filePath) throws IOException {
        Path path = Paths.get(AppConfig.ROOT + File.separator + filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        String fileExtension = getExtension(filePath);
        if (isSupportedFileFormat(getExtension(filePath))) {
            try (Stream<String> lines = Files.lines(path)) {
                return lines.collect(Collectors.joining("\n"));
            }
        } else if (isSupportedImageFormat(fileExtension)) {
            return ImageIO.read(path.toFile());
        } else {
            throw new IOException("Unsupported file type: " + filePath);
        }
    }

    public int fileHash(String filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(filePath.getBytes(StandardCharsets.UTF_8));

            byte[] hashBytes = digest.digest();
            BigInteger hashValue = new BigInteger(1, hashBytes);

            return hashValue.mod(BigInteger.valueOf(ChordState.CHORD_SIZE)).intValue();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error using SHA-256");
        }
    }

    public int fileHash2(FileInfo fileInfo) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            if (fileInfo.getType() == FileType.TEXT) {
                // Byte-based hashing approach
                String textContent = ((TextFileInfo) fileInfo).getFileContent();
                digest.update(textContent.getBytes(StandardCharsets.UTF_8));
            } else if (fileInfo.getType() == FileType.IMAGE) {
                // Hash the pixel data
                BufferedImage image = ((ImageFileInfo) fileInfo).getFileContent();
                int width = image.getWidth();
                int height = image.getHeight();
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int pixel = image.getRGB(x, y);
                        digest.update((byte) ((pixel >> 16) & 0xff)); // Red
                        digest.update((byte) ((pixel >> 8) & 0xff));  // Green
                        digest.update((byte) (pixel & 0xff));         // Blue
                    }
                }
            } else {
                throw new IOException("Unsupported file type for hashing: " + fileInfo.getPath());
            }

            byte[] hashBytes = digest.digest();
            BigInteger hashValue = new BigInteger(1, hashBytes);

            return hashValue.mod(BigInteger.valueOf(ChordState.CHORD_SIZE)).intValue();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error using SHA-256");
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    /**
     * @return file map if the ip and port matches, null otherwise
     */
    public Map<String, FileInfo> getFilesIfIpPortMatches(String ip, int port, int requesterId) {
        return sameIpAndPort(ip, port)
                ? friendManager.isFriend(requesterId)
                ? fileMap
                : fileMap.entrySet().stream().collect(Collectors.filtering(
                entry -> entry.getValue().getVisibility().equals(PUBLIC),
                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                : null;
    }

    public Map<String, FileInfo> getFiles() {
        return fileMap;
    }

    public void setFiles(Map<String, FileInfo> myFiles) {
        this.fileMap = myFiles;
    }

    public void printFiles(Map<String, FileInfo> fileMap) {
        for (Map.Entry<String, FileInfo> entry : fileMap.entrySet()) {
            String filePath = entry.getKey();
            FileInfo fileInfo = entry.getValue();
            AppConfig.timestampedStandardPrint("- " + filePath + " (" + fileInfo.getVisibility() + ")");
        }
    }

    public int removeOriginalFile(String filePath) {
        return fileMap.remove(filePath) != null ? -2 : 0;
    }

    public void init(WelcomeMessage welcomeMsg) {
        this.fileMap = welcomeMsg.getFiles();
    }

    public boolean containsFile(FileInfo fileInfo) {
        return fileMap.containsKey(fileInfo.getPath());
    }

    public boolean containsFile(String filePath) {
        return fileMap.containsKey(filePath);
    }

    public String getFileVisibility(String filePath) {
        return fileMap.containsKey(filePath)
                ? fileMap.get(filePath).getVisibility()
                : "";
    }

    public FileInfo getFile(String filePath) {
        return fileMap.get(filePath);
    }

    private boolean sameIpAndPort(String ip, int port) {
        return AppConfig.myServentInfo.getIpAddress().equals(ip) && AppConfig.myServentInfo.getListenerPort() == port;
    }

    public String getExtension(String filePath) {
        String fileName = new File(filePath).getName();
        int lastIndexOfDot = fileName.lastIndexOf(".");
        return lastIndexOfDot == -1 ? "" : fileName.substring(lastIndexOfDot + 1);
    }

    public boolean isSupportedImageFormat(String fileExtension) {
        return SUPPORTED_IMG_EXTENSIONS.contains(fileExtension.toLowerCase());
    }

    public boolean isSupportedFileFormat(String fileExtension) {
        return SUPPORTED_FILE_EXTENSIONS.contains(fileExtension.toLowerCase());
    }

    public FileType getFileType(String filePath) throws IOException {
        String extension = getExtension(filePath);
        if (isSupportedFileFormat(extension)) {
            return FileType.TEXT;
        } else if (isSupportedImageFormat(extension)) {
            return FileType.IMAGE;
        }
        throw new IOException("Unsupported file type;");
    }

    public FileInfo getInfoWithIncrementedBackupId(FileInfo backupFile) {
        if (backupFile.getType() == FileType.IMAGE) {
            return new ImageFileInfo(
                    backupFile.getPath(),
                    backupFile.getExt(),
                    (BufferedImage) backupFile.getFileContent(),
                    backupFile.getVisibility(),
                    backupFile.getOwnerKey(),
                    backupFile.getBackupId() + 1
            );
        } else {
            return new TextFileInfo(
                    backupFile.getPath(),
                    backupFile.getExt(),
                    (String) backupFile.getFileContent(),
                    backupFile.getVisibility(),
                    backupFile.getOwnerKey(),
                    backupFile.getBackupId() + 1
            );
        }
    }
}
