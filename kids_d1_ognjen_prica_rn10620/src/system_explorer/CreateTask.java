package system_explorer;

import task.Task;
import task.TaskType;

import java.io.File;

public record CreateTask(File matrixFile) implements Task {

    @Override
    public TaskType getType() {
        return TaskType.CREATE;
    }
}
