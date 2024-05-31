package queue;

import task.Task;

public interface TaskQueue {
    void enqueue(Task task) throws InterruptedException;

    Task dequeue() throws InterruptedException;

    boolean isEmpty();
}
