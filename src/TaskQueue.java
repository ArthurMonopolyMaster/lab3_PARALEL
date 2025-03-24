import java.util.*;
import java.util.concurrent.locks.*;
import java.util.function.Function;

class TaskQueue {
    private final Queue<Task<?>> queue = new LinkedList<>();
    private final int maxSize;
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private int missingTasks = 0;
    private boolean isFull = false;
    private long fullTime = 0;

    public TaskQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return queue.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public boolean offer(Task<?> task) {
        lock.lock();
        try {
            if (queue.size() >= maxSize) {
                if (!isFull) {
                    isFull = true;
                    fullTime = System.currentTimeMillis();
                }
                missingTasks++;
                return false;
            }
            queue.offer(task);
            notFull.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }
    public Task<?> poll() {
        lock.lock();
        try {
            return queue.poll();
        } finally {
            lock.unlock();
        }
    }

    public int getMissingTasks() {
        lock.lock();
        try {
            return missingTasks;
        } finally {
            lock.unlock();
        }
    }

    public long getFullTime() {
        lock.lock();
        try {
            return fullTime;
        } finally {
            lock.unlock();
        }
    }

    // Вбудований клас Task

}
