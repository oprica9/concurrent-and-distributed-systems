package matrix_multiplier;

import model.Matrix;
import task.Task;
import task.TaskType;

public class MultiplyTask implements Task {

    private final Matrix matrix1;
    private final Matrix matrix2;
    private final String customName;
    private final boolean async;

    public MultiplyTask(Matrix matrix1, Matrix matrix2, boolean async) {
        this.matrix1 = matrix1;
        this.matrix2 = matrix2;
        this.async = async;
        this.customName = matrix1.getName() + matrix2.getName();
    }

    public MultiplyTask(Matrix matrix1, Matrix matrix2, String customName, boolean async) {
        this.matrix1 = matrix1;
        this.matrix2 = matrix2;
        if (customName == null) {
            this.customName = matrix1.getName() + matrix2.getName();
        } else {
            this.customName = customName;
        }
        this.async = async;
    }

    public Matrix[] getMatrices() {
        return new Matrix[]{matrix1, matrix2};
    }

    public String getCustomName() {
        return customName;
    }

    @Override
    public TaskType getType() {
        return TaskType.MULTIPLY;
    }

    public boolean isAsync() {
        return async;
    }
}
