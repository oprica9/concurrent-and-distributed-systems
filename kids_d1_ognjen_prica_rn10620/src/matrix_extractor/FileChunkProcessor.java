package matrix_extractor;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class FileChunkProcessor implements Callable<List<MatrixUpdate>> {
    private final String filePath;
    private final long startByte;
    private final long endByte;

    public FileChunkProcessor(String filePath, long startByte, long endByte) {
        this.filePath = filePath;
        this.startByte = startByte;
        this.endByte = endByte;
    }

    @Override
    public List<MatrixUpdate> call() throws Exception {
        List<MatrixUpdate> updates = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            file.seek(startByte);

            // Handling for chunks not starting from the first byte
            if (startByte != 0) {
                // Ensure we start at the beginning of a line
                skipToNextLine(file);
            }

            String line;
            while (file.getFilePointer() < endByte && (line = file.readLine()) != null) {
                processLine(line, updates);
            }
        }
        return updates;
    }

    private void skipToNextLine(RandomAccessFile file) throws IOException {
        // Move slightly back to ensure not missing the line break check
        long backtrackPosition = Math.max(file.getFilePointer() - 1, 0);
        file.seek(backtrackPosition);

        int byteData;
        // Read byte by byte to find the line terminator and position the file pointer at the start of the next line
        while ((byteData = file.read()) != -1) {
            if (byteData == '\n') {
                break; // Stop when a newline is found, file pointer is now at the start of the next line
            }
        }
    }

    private void processLine(String line, List<MatrixUpdate> updates) {
        if (line == null || !line.contains("=")) return;
        String[] parts = line.trim().split("[ ,=]+");
        if (parts.length < 3) {
            System.err.println("Incomplete line: " + line);
            return; // Skip if the line does not have enough parts
        }
        try {
            int row = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]);
            BigInteger value = new BigInteger(parts[2]);

            updates.add(new MatrixUpdate(row, col, value));
        } catch (NumberFormatException e) {
            System.err.println("Error parsing line: " + line);
        }
    }
}
