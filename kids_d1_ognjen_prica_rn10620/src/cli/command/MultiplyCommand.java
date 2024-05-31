package cli.command;

import cli.Command;
import exceptions.MatrixDimensionException;
import matrix_brain.MatrixBrain;
import matrix_multiplier.MultiplyTask;
import model.Matrix;
import queue.TaskQueue;

import static context.Locks.coordinatorLock;

public class MultiplyCommand implements Command {
    private final TaskQueue taskQueue;
    private final MatrixBrain matrixBrain;

    public MultiplyCommand(TaskQueue taskQueue, MatrixBrain matrixBrain) {
        this.taskQueue = taskQueue;
        this.matrixBrain = matrixBrain;
    }

    @Override
    public String commandName() {
        return "multiply";
    }

    @Override
    public void execute(String args) {
        String[] splitArgs = args.split(" ");
        if (splitArgs.length < 1 || splitArgs.length > 3) {
            System.out.println("Invalid arguments for multiply. Usage: multiply <matrix1_name,matrix2_name> [-name <product_name>] [-async]");
            return;
        }

        String mat1Name;
        String mat2Name;
        boolean async = false;
        String newMatrixName;

        if (!splitArgs[0].contains(",")) {
            throw new IllegalArgumentException("You have to specify two matrices you want to multiply separated by a comma.");
        }

        String[] matrixNames = splitArgs[0].split(",");
        mat1Name = matrixNames[0];
        mat2Name = matrixNames[1];
        newMatrixName = mat1Name + mat2Name;

        for (int i = 1; i < splitArgs.length; i++) {
            switch (splitArgs[i]) {
                case "-async" -> async = setAsync(async);
                case "-name" -> {
                    if (i + 1 >= splitArgs.length) {
                        throw new IllegalArgumentException("You have to specify a matrix name after '-name'");
                    }
                    newMatrixName = splitArgs[++i];
                }
                default -> throw new IllegalArgumentException("Unknown arguments.");
            }
        }

        // Fetch matrices by name from MatrixBrain
        Matrix m1 = matrixBrain.getMatrix(mat1Name);
        Matrix m2 = matrixBrain.getMatrix(mat2Name);

        if (m1 == null) {
            System.out.printf("Error: %s not found.\n", mat1Name);
            return;
        }
        if (m2 == null) {
            System.out.printf("Error: %s not found.\n", mat2Name);
            return;
        }

        // we got it
        if (matrixBrain.isMultiplied(m1, m2)) {
            try {
                System.out.println(matrixBrain.getProduct(m1, m2, newMatrixName).getData());
            } catch (MatrixDimensionException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        // it's getting there
        if (matrixBrain.isMultiplicationOngoing(m1, m2)) {
            System.out.println("ongoing");
            if (async) {
                System.out.println("async");
                System.out.println("Slow down there buckaroo, its multiplying...");
                return;
            }

            Matrix matrix;
            try {
                matrix = matrixBrain.getProduct(m1, m2, newMatrixName);
            } catch (MatrixDimensionException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Multiplied! The result is..." + matrix.getName());
            return;
        }

        if (matrixBrain.isMultiplied(m1, m2)) {
            try {
                System.out.println(matrixBrain.getProduct(m1, m2, newMatrixName).getData());
            } catch (MatrixDimensionException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        // we don't got it ;(
        System.out.println("Multiplying " + m1.getName() + " and " + m2.getName() + " for the first time.");
        if (async) {
            try {
                synchronized (coordinatorLock) {
                    taskQueue.enqueue(new MultiplyTask(m1, m2, newMatrixName, true));
                    coordinatorLock.notifyAll();
                }
                return;
            } catch (InterruptedException e) {
                System.out.println("Unexpected error occurred: " + e.getMessage());
            }
        }

        try {
            Matrix matrix = matrixBrain.getProduct(m1, m2, newMatrixName);
            System.out.println("Multiplied! The result is..." + matrix.getName());
        } catch (MatrixDimensionException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean setAsync(boolean async) {
        if (async) {
            throw new IllegalArgumentException("'-async' already specified.");
        }
        return true;
    }
}
