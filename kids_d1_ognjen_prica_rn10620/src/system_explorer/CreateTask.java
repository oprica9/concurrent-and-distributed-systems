package system_explorer;

import task.Task;
import task.TaskType;

import java.io.File;

public class CreateTask implements Task {

    private final File matrixFile;

    public CreateTask(File matrixFile) {
        this.matrixFile = matrixFile;
    }

    public File getMatrixFile() {
        return matrixFile;
    }

    @Override
    public TaskType getType() {
        return TaskType.CREATE;
    }
}
