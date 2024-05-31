package matrix_multiplier;

import app.Cancellable;
import exceptions.MatrixDimensionException;
import matrix_brain.MatrixBrain;
import model.Matrix;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MatrixMultiplier implements Cancellable {

    private final MatrixBrain matrixBrain;
    private final ExecutorService threadPool;
    private final int limit;

    public MatrixMultiplier(MatrixBrain matrixBrain, int limit) {
        this.matrixBrain = matrixBrain;
        this.threadPool = Executors.newWorkStealingPool();
        this.limit = limit;
    }

    public Future<Matrix> multiplyMatrices(MultiplyTask task) throws MatrixDimensionException {
        Future<Matrix> matrixFuture = threadPool.submit(() -> {
            Matrix matrix1 = task.getMatrices()[0];
            Matrix matrix2 = task.getMatrices()[1];
            String name = task.getCustomName();

            if (matrix1.getCols() != matrix2.getRows()) {
                throw new MatrixDimensionException("Matrix dimensions do not match for multiplication.");
            }

            List<Callable<Matrix>> partTasks = new ArrayList<>();
            int rowsPerTask = matrix1.getRows() / limit;
            for (int i = 0; i < limit; i++) {
                final int startRow = i * rowsPerTask;
                final int endRow = (i == limit - 1) ? matrix1.getRows() : startRow + rowsPerTask;
                partTasks.add(new MatrixMultiplicationTask(matrix1, matrix2, startRow, endRow));
            }

            BigInteger[][] finalProduct = new BigInteger[matrix1.getRows()][matrix2.getCols()];

            List<Future<Matrix>> futures = threadPool.invokeAll(partTasks);
            for (int i = 0; i < futures.size(); i++) {
                Matrix partialResult = futures.get(i).get();
                int startRow = i * rowsPerTask;
                System.arraycopy(partialResult.getMatrix(), 0, finalProduct, startRow, partialResult.getMatrix().length);
            }

            // Optionally add the result to MatrixBrain here or handle it outside based on the future's completion
            return new Matrix(name, finalProduct);
        });

        if (task.isAsync()) {
            matrixBrain.registerTask(task.getMatrices()[0].getName() + task.getMatrices()[1].getName(), task.getCustomName(), matrixFuture);
        }

        return matrixFuture;
    }

    @Override
    public void stop() {
        threadPool.shutdown();
    }
}
