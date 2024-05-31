package queue;

import task.Task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BlockingTaskQueue implements TaskQueue {

    private final BlockingQueue<Task> blockingQueue;

    public BlockingTaskQueue() {
        this.blockingQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void enqueue(Task task) throws InterruptedException {
        blockingQueue.put(task);
    }

    @Override
    public Task dequeue() throws InterruptedException {
        return blockingQueue.take();
    }

    @Override
    public boolean isEmpty() {
        return blockingQueue.isEmpty();
    }
}
