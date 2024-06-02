package matrix_multiplier;

import model.Matrix;

import java.math.BigInteger;
import java.util.concurrent.Callable;

public class MatrixMultiplicationTask implements Callable<Matrix> {
    private final Matrix matrix1;
    private final Matrix matrix2;
    private final int startRow;
    private final int endRow;

    public MatrixMultiplicationTask(Matrix matrix1, Matrix matrix2, int startRow, int endRow) {
        this.matrix1 = matrix1;
        this.matrix2 = matrix2;
        this.startRow = startRow;
        this.endRow = endRow;
    }

    @Override
    public Matrix call() {
        int cols2 = matrix2.getCols();
        int cols1 = matrix1.getCols();
        BigInteger[][] product = new BigInteger[endRow - startRow][cols2];

        for (int i = startRow; i < endRow; i++) {
            for (int j = 0; j < cols2; j++) {
                BigInteger sum = BigInteger.ZERO;
                for (int k = 0; k < cols1; k++) {
                    sum = sum.add(matrix1.get(i, k).multiply(matrix2.get(k, j)));
                }
                product[i - startRow][j] = sum;
            }
        }

        return new Matrix("Partial", product);
    }
}
