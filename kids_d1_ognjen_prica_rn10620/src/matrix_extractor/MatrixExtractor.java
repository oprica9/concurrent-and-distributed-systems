package matrix_extractor;

import app.Cancellable;
import matrix_brain.MatrixBrain;
import matrix_multiplier.MultiplyTask;
import model.Matrix;
import queue.TaskQueue;
import system_explorer.CreateTask;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static context.Locks.coordinatorLock;

public class MatrixExtractor implements Cancellable {

    private final MatrixBrain matrixBrain;
    private final TaskQueue taskQueue;
    private final int chunkSizeLimit;
    private final ExecutorService threadPool;

    public MatrixExtractor(MatrixBrain matrixBrain, TaskQueue taskQueue, int chunkSizeLimit) {
        this.matrixBrain = matrixBrain;
        this.taskQueue = taskQueue;
        this.chunkSizeLimit = chunkSizeLimit;
        this.threadPool = Executors.newWorkStealingPool();
    }

    @Override
    public void stop() {
        threadPool.shutdown();
    }

    public void extractMatrix(CreateTask task) {
        System.out.println("Extracting... " + task.matrixFile());
        File file = task.matrixFile();
        long fileSize = file.length();
        String matrixFileLocation = file.getAbsolutePath();

        String matrixName;
        int rows, cols;
        long startByteForProcessing = 0;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            // Read and parse the first line for metadata
            String firstLine = raf.readLine();
            String[] metadataParts = firstLine.split(", ");
            matrixName = metadataParts[0].split("=")[1];
            rows = Integer.parseInt(metadataParts[1].split("=")[1]);
            cols = Integer.parseInt(metadataParts[2].split("=")[1]);

            // Calculate the starting byte for chunk processing, skipping the first line
            startByteForProcessing = raf.getFilePointer();
        } catch (IOException e) {
            throw new RuntimeException("Error reading matrix file", e);
        }

        Matrix matrix = new Matrix(matrixName, rows, cols, matrixFileLocation);

        long numberOfChunks = (fileSize - startByteForProcessing) / chunkSizeLimit + ((fileSize - startByteForProcessing) % chunkSizeLimit == 0 ? 0 : 1);

        List<Future<List<MatrixUpdate>>> futures = new ArrayList<>();
        for (int i = 0; i < numberOfChunks; i++) {
            long startByte = startByteForProcessing + (long) i * chunkSizeLimit;
            long endByte = Math.min(startByte + chunkSizeLimit, fileSize);
            Callable<List<MatrixUpdate>> callable = new FileChunkProcessor(file.getAbsolutePath(), startByte, endByte);
            futures.add(threadPool.submit(callable));
        }

        // Process future results and update matrix
        futures.forEach(future -> {
            try {
                List<MatrixUpdate> updates = future.get();
                updates.forEach(update -> matrix.insert(update.row(), update.col(), update.value()));
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error processing matrix update tasks", e);
            }
        });

        matrixBrain.completeTask(matrix.getName(), matrix);
        createSquareMatrixTask(matrix);
    }

    private void createSquareMatrixTask(Matrix matrix) {
        try {
            taskQueue.enqueue(new MultiplyTask(matrix, matrix, true));
            synchronized (coordinatorLock) {
                coordinatorLock.notifyAll();
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
