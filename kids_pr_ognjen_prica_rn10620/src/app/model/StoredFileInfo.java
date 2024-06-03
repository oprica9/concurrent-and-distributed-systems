package app.model;

import java.io.Serial;
import java.io.Serializable;

public class StoredFileInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 2004343237449837768L;

    private final String path;
    private final String fileContent;
    private final String visibility;
    private final int ownerKey;
    private int backupId;

    public StoredFileInfo(String path, String fileContent, String visibility, int ownerKey) {
        this.path = path;
        this.fileContent = fileContent;
        this.visibility = visibility;
        this.ownerKey = ownerKey;
        this.backupId = 0;
    }

    public StoredFileInfo(String path, String fileContent, String visibility, int ownerKey, int backupId) {
        this.path = path;
        this.fileContent = fileContent;
        this.visibility = visibility;
        this.ownerKey = ownerKey;
        this.backupId = backupId;
    }

    public String getFileContent() {
        return fileContent;
    }

    public String getVisibility() {
        return visibility;
    }

    public int getOwnerKey() {
        return ownerKey;
    }

    public int getBackupId() {
        return backupId;
    }

    public void setBackupId(int backupId) {
        this.backupId = backupId;
    }

    public String getPath() {
        return path;
    }
}
