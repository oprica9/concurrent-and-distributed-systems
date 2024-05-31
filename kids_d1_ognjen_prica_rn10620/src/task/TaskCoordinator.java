package task;

import app.Cancellable;
import exceptions.MatrixDimensionException;
import matrix_extractor.MatrixExtractor;
import matrix_multiplier.MatrixMultiplier;
import matrix_multiplier.MultiplyTask;
import queue.TaskQueue;
import system_explorer.CreateTask;

import static context.Locks.coordinatorLock;

public class TaskCoordinator implements Runnable, Cancellable {

    private volatile boolean running;
    private final TaskQueue taskQueue;
    private final MatrixExtractor matrixExtractor;
    private final MatrixMultiplier matrixMultiplier;

    public TaskCoordinator(TaskQueue taskQueue, MatrixExtractor matrixExtractor, MatrixMultiplier matrixMultiplier) {
        this.taskQueue = taskQueue;
        this.matrixExtractor = matrixExtractor;
        this.matrixMultiplier = matrixMultiplier;
    }

    @Override
    public void run() {
        System.out.println("coordinating...");
        running = true;
        while (running) {
            synchronized (coordinatorLock) {
                while (taskQueue.isEmpty()) {
                    try {
                        System.out.println("Sleeping");
                        coordinatorLock.wait();
                    } catch (InterruptedException e) {
                        System.out.println("Woken up?");
                        Thread.currentThread().interrupt();
                        System.out.println(e.getMessage());
                    }
                }
            }

            try {
                Task task = taskQueue.dequeue();
                if (task.getType() == TaskType.CREATE) {
                    matrixExtractor.extractMatrix((CreateTask) task);
                } else if (task.getType() == TaskType.MULTIPLY) {
                    matrixMultiplier.multiplyMatrices((MultiplyTask) task);
                } else if (task.getType() == TaskType.POISON) {
                    // stop
                    matrixExtractor.stop();
                    matrixMultiplier.stop();
                    running = false;
                }
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
                System.exit(-4);
            } catch (MatrixDimensionException e) {
                System.out.println(e.getMessage());
            }
        }
        System.out.println("Stopping TaskCoordinator...");
    }

    @Override
    public void stop() {
        synchronized (coordinatorLock) {
            try {
                taskQueue.enqueue(new PoisonTask());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            coordinatorLock.notifyAll();
        }
    }
}
