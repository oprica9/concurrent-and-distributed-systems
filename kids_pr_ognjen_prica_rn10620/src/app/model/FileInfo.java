package app.model;

import java.io.Serial;
import java.io.Serializable;

public abstract class FileInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 2004343237449837768L;
    private final String path;
    private final FileType type;
    private final String ext;
    private final String visibility;
    private final int ownerKey;
    private final int backupId;

    public FileInfo(String path, String ext, FileType type, String visibility, int ownerKey, int backupId) {
        this.path = path;
        this.ext = ext;
        this.type = type;
        this.visibility = visibility;
        this.ownerKey = ownerKey;
        this.backupId = backupId;
    }

    public String getPath() {
        return path;
    }

    public String getExt() {
        return ext;
    }

    public FileType getType() {
        return type;
    }

    public abstract Object getFileContent();

    public String getVisibility() {
        return visibility;
    }

    public int getOwnerKey() {
        return ownerKey;
    }

    public int getBackupId() {
        return backupId;
    }

}
