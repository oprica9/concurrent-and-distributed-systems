package task;

public class PoisonTask implements Task {
    @Override
    public TaskType getType() {
        return TaskType.POISON;
    }
}
