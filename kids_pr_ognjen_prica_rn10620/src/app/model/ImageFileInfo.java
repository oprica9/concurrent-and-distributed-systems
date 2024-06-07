package app.model;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;

public class ImageFileInfo extends FileInfo {
    @Serial
    private static final long serialVersionUID = 3656571244776550644L;
    private transient BufferedImage imageContent;

    public ImageFileInfo(String path, String ext, BufferedImage imageContent, String visibility, int ownerKey, int backupId) {
        super(path, ext, FileType.IMAGE, visibility, ownerKey, backupId);
        this.imageContent = imageContent;
    }

    @Override
    public BufferedImage getFileContent() {
        return imageContent;
    }

    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        ImageIO.write(imageContent, getExt(), out);
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        imageContent = ImageIO.read(in);
    }
}