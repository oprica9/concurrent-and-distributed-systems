package app.model;

import java.io.Serial;

public class TextFileInfo extends FileInfo {
    @Serial
    private static final long serialVersionUID = 5003069788156698933L;
    private final String textContent;

    public TextFileInfo(String path, String ext, String textContent, String visibility, int ownerKey, int backupId) {
        super(path, ext, FileType.TEXT, visibility, ownerKey, backupId);
        this.textContent = textContent;
    }

    @Override
    public String getFileContent() {
        return textContent;
    }
}
