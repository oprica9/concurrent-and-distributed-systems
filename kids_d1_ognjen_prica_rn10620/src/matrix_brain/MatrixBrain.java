package matrix_brain;


import app.Cancellable;
import exceptions.MatrixDimensionException;
import exceptions.MatrixDoesntExistException;
import matrix_multiplier.MatrixMultiplier;
import matrix_multiplier.MultiplyTask;
import model.Matrix;
import model.MatrixInfo;
import model.MatrixUtil;
import system_explorer.SystemExplorer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class MatrixBrain implements Cancellable {
    private final ExecutorService threadPool;
    private final Map<String, Future<Matrix>> ongoingTasks;
    private final List<Matrix> completedMatrices;
    private MatrixMultiplier matrixMultiplier;
    private SystemExplorer systemExplorer;
    private final Map<String, String> productsAndNames;
    private final Map<String, String> fileNameMatrixName;

    public MatrixBrain() {
        this.threadPool = Executors.newWorkStealingPool();
        this.ongoingTasks = new ConcurrentHashMap<>();
        this.completedMatrices = new ArrayList<>();
        this.productsAndNames = new HashMap<>();
        this.fileNameMatrixName = new HashMap<>();
    }

    @Override
    public void stop() {
        System.out.println("Stopping Matrix Brain...");
        threadPool.shutdown();
    }

    public void registerTask(String identifier, String customName, Future<Matrix> task) {
        ongoingTasks.put(identifier, task);
        productsAndNames.put(identifier, customName);
    }

    public void completeTask(String identifier, Matrix matrix) {
        ongoingTasks.remove(identifier);
        if (completedMatrices.contains(matrix)) {
            // update existing
            int i = completedMatrices.indexOf(matrix);
            completedMatrices.get(i).update(matrix);
        } else {
            completedMatrices.add(matrix);
            if (matrix.getLocation() != null) {
                fileNameMatrixName.put(matrix.getLocation().substring(matrix.getLocation().lastIndexOf("\\") + 1), matrix.getName());
            }
        }
    }

    public Map<String, Future<Matrix>> getOngoingTasks() {
        return ongoingTasks;
    }

    public boolean isMultiplicationOngoing(Matrix m1, Matrix m2) {
        String name = m1.getName() + m2.getName();

        if (getOngoingTasks().containsKey(name)) {
            if (getOngoingTasks().get(name).isDone()) {
                Matrix matrix;
                try {
                    matrix = getOngoingTasks().get(name).get();
                    productsAndNames.put(m1.getName() + m2.getName(), name);
                    completeTask(matrix.getName(), matrix);
                } catch (InterruptedException | ExecutionException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted", e);
                }
            }
        }

        return getOngoingTasks().containsKey(name);
    }

    public boolean isMultiplied(Matrix m1, Matrix m2) {
        String identifier = productsAndNames.getOrDefault(m1.getName() + m2.getName(), m1.getName() + m2.getName());
        return completedMatrices.stream().anyMatch(m -> m.getName().equals(identifier));
    }

    public Matrix getProduct(Matrix m1, Matrix m2, String name) throws MatrixDimensionException {
        if (isMultiplied(m1, m2)) {
            return getCompleted(m1, m2, name);
        }

        if (isMultiplicationOngoing(m1, m2)) {
            return getOngoing(m1, m2);
        }

        Matrix matrix;
        Future<Matrix> matrixFuture = matrixMultiplier.multiplyMatrices(new MultiplyTask(m1, m2, name, false));
        try {
            matrix = matrixFuture.get();
            if (name == null)
                productsAndNames.put(m1.getName() + m2.getName(), m1.getName() + m2.getName());
            else {
                productsAndNames.put(m1.getName() + m2.getName(), name);
            }
            completeTask(matrix.getName(), matrix);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            while (cause instanceof RuntimeException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause instanceof MatrixDimensionException) {
                throw (MatrixDimensionException) cause;
            } else {
                throw new RuntimeException("Error during matrix multiplication", e);
            }
        }
        return matrix;
    }

    public List<MatrixInfo> getAllInfo(String sortOrder) {
        List<MatrixInfo> matrixInfos = new ArrayList<>();

        for (Matrix matrix : completedMatrices) {
            matrixInfos.add(new MatrixInfo(matrix.getName(), matrix.getRows(), matrix.getCols(), matrix.getLocation()));
        }

        if (sortOrder != null)
            sortInfos(matrixInfos, sortOrder);

        return matrixInfos;
    }

    public List<MatrixInfo> getFirstNInfo(int firstN, String sortOrder) {
        List<MatrixInfo> matrixInfos = new ArrayList<>();

        for (int i = 0; i < completedMatrices.size() && i < firstN; i++) {
            Matrix matrix = completedMatrices.get(i);
            matrixInfos.add(new MatrixInfo(matrix.getName(), matrix.getRows(), matrix.getCols(), matrix.getLocation()));
        }
        if (sortOrder != null)
            sortInfos(matrixInfos, sortOrder);

        return matrixInfos;
    }

    public List<MatrixInfo> getLastNInfo(int lastN, String sortOrder) {
        List<MatrixInfo> matrixInfos = new ArrayList<>();

        for (int i = completedMatrices.size() - 1; i >= completedMatrices.size() - lastN; i--) {
            Matrix matrix = completedMatrices.get(i);
            matrixInfos.add(new MatrixInfo(matrix.getName(), matrix.getRows(), matrix.getCols(), matrix.getLocation()));
        }

        if (sortOrder != null)
            sortInfos(matrixInfos, sortOrder);

        return matrixInfos;
    }

    public MatrixInfo getInfo(String matrixName) throws MatrixDoesntExistException {
        for (Matrix m : completedMatrices) {
            if (m.getName().equals(matrixName)) {
                return new MatrixInfo(m.getName(), m.getRows(), m.getCols(), m.getLocation());
            }
        }
        throw new MatrixDoesntExistException("Matrix with name: " + matrixName + " doesn't exist.");
    }

    public Matrix getMatrix(String matrixName) {
        for (Matrix m : completedMatrices) {
            if (m.getName().equals(matrixName)) {
                return m;
            }
        }
        return null;
    }

    public void setMultiplier(MatrixMultiplier matrixMultiplier) {
        this.matrixMultiplier = matrixMultiplier;
    }

    private Matrix getCompleted(Matrix m1, Matrix m2, String name) {
        String identifier = productsAndNames.getOrDefault(m1.getName() + m2.getName(), name);

        Matrix matrix = null;
        for (Matrix m : completedMatrices) {
            if (m.getName().equals(identifier)) {
                matrix = m;
            }
        }
        return matrix;
    }

    private Matrix getOngoing(Matrix m1, Matrix m2) {
        try {
            Matrix matrix = ongoingTasks.get(m1.getName() + m2.getName()).get();
            completeTask(matrix.getName(), matrix);
            return matrix;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void sortInfos(List<MatrixInfo> matrixInfos, String sortOrder) {

        boolean asc = sortOrder.equals("asc");

        matrixInfos.sort((info1, info2) -> {
            int rowComparison = Integer.compare(info1.rows(), info2.rows());
            if (rowComparison != 0) {
                return asc ? rowComparison : -rowComparison;
            } else {
                int colComparison = Integer.compare(info1.cols(), info2.cols());
                return asc ? colComparison : -colComparison;
            }
        });
    }

    public void saveMatrixToFile(String matrixName, String fileName) {
        Matrix matrix = getMatrix(matrixName);
        if (matrix == null) {
            throw new RuntimeException("Matrix with name: " + matrixName + " doesn't exist.");
        }
        threadPool.submit(() -> {
            File file = MatrixUtil.saveMatrix(matrix, fileName);
            String path = file.getAbsolutePath();
            matrix.setLocation(path);
            fileNameMatrixName.put(path.substring(path.lastIndexOf("\\") + 1), matrix.getName());
            systemExplorer.addFileToMap(file);
        });
    }

    public void clearMatrix(String name) {
        int i;
        Matrix toRemove = null;
        for (i = 0; i < completedMatrices.size(); i++) {
            if (completedMatrices.get(i).getName().equals(name)) {
                toRemove = completedMatrices.get(i);
                break;
            }
        }
        if (toRemove != null) {
            completedMatrices.remove(i);
            productsAndNames.remove(name);
            String path = toRemove.getLocation().substring(toRemove.getLocation().lastIndexOf("\\") + 1);
            systemExplorer.removeFileFromMap(path);
        }
    }


    public void setSystemExplorer(SystemExplorer systemExplorer) {
        this.systemExplorer = systemExplorer;
    }
}
