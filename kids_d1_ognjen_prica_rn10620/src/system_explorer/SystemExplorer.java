package system_explorer;

import app.Cancellable;
import queue.TaskQueue;
import task.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static context.Locks.coordinatorLock;

public class SystemExplorer implements Runnable, Cancellable {

    private volatile boolean running;
    private final List<String> directories;
    private final Map<String, Long> fileMap;
    private final TaskQueue taskQueue;
    private final int sleepTime;

    public SystemExplorer(int sleepTime, String initialDir, TaskQueue taskQueue) {
        this.directories = new ArrayList<>();
        this.directories.add(initialDir);
        this.sleepTime = sleepTime;
        this.fileMap = new HashMap<>();
        this.taskQueue = taskQueue;
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            searchDirectories();
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println(e.getMessage());
                System.exit(-3);
            }
        }
        System.out.println("Stopping System Explorer...");
    }

    @Override
    public void stop() {
        running = false;
    }

    public void addDirectory(String dir) {
        if (!directories.contains(dir)) {
            directories.add(dir);
        }
    }

    public void addFileToMap(File file) {
        this.fileMap.put(file.getName(), file.lastModified());
    }

    public void removeFileFromMap(String fileName) {
        fileMap.remove(fileName);
    }

    private void searchDirectories() {
        File[] files = new File[directories.size()];
        int i = 0;
        for (String dir : directories) {
            files[i++] = new File(dir);
        }
        digFiles(files);
    }

    private void digFiles(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                File[] list = file.listFiles();
                if (list != null) {
                    digFiles(list);
                }
            } else if (file.isFile()) {
                if (!file.getName().substring(file.getName().lastIndexOf(".")).equals(".rix")) {
                    continue;
                }
                if (fileMap.containsKey(file.getName()) && fileMap.get(file.getName()).equals(file.lastModified())) {
                    continue;
                }
                System.out.println("Exploring " + file.getName());
                // add file and create task
                fileMap.put(file.getName(), file.lastModified());
                createTask(file);
            }
        }
    }

    private void createTask(File file) {
        Task task = new CreateTask(file);
        try {
            taskQueue.enqueue(task);
            synchronized (coordinatorLock) {
                coordinatorLock.notifyAll();
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
            System.exit(-2);
        }
    }
}
