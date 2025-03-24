import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

class ThreadPool<Result> {
    private final int workerCount;
    private final TaskQueue taskQueue;
    private final List<Thread> workers = new ArrayList<>();
    private final Map<Integer, Result> results = new ConcurrentHashMap<>();
    private final Object lock = new Object();
    private volatile boolean terminated = false;
    private volatile boolean paused = false;
    private int lastId = 0;

    public ThreadPool(int workerCount) {
        this.workerCount = workerCount;
        this.taskQueue = new TaskQueue(20); // Обмежена черга задач
    }

    public void initialize() {
        for (int i = 0; i < workerCount; i++) {
            Thread worker = new Thread(this::routine);
            workers.add(worker);
            worker.start();
        }
    }

    public void terminate() {
        synchronized (lock) {
            terminated = true;
            lock.notifyAll();
        }
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                System.out.println("Worker interrupted during termination.");
            }
        }
        System.out.println("Thread pool terminated.");
    }

    public void pause() {
        synchronized (lock) {
            paused = true;
        }
        System.out.println("Paused");
    }

    public void resume() {
        synchronized (lock) {
            paused = false;
            lock.notifyAll();
        }
        System.out.println("Resumed");
    }

    private void routine() {
        while (true) {
            Task<Result> task;
            synchronized (lock) {
                while ((paused || taskQueue.isEmpty()) && !terminated) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
                if (terminated) return;

                task = (Task<Result>) taskQueue.poll();
                if (task == null) continue;
            }

            int id = task.getId();
            try {
                Result result = task.execute();
                results.put(id, result);
                System.out.println("Task " + id + " completed with result: " + result);
            } catch (Exception e) {
                System.out.println("Task " + id + " failed: " + e.getMessage());
            }

            try {
                Thread.sleep(5000 + new Random().nextInt(6000)); // 5-10 сек затримка
            } catch (InterruptedException e) {
                System.out.println("Task " + id + " interrupted.");
                return;
            }
        }
    }

    public int addTask(Function<Integer[], Result> function, Integer... parameters) {
        synchronized (lock) {
            lastId++;
            Task<Result> task = new Task<>(lastId, function, parameters);
            if (!taskQueue.offer(task)) {
                System.out.println("Task " + lastId + " rejected (queue is full)");
                return -1;
            }
            lock.notifyAll();
        }
        return lastId;
    }

    public Result getResult(int id) {
        return results.get(id);
    }

    public TaskQueue getTaskQueue() {
        return taskQueue;
    }
}

class Task<Result> {
    private final int id;
    private final Function<Integer[], Result> function;
    private final Integer[] parameters;

    public Task(int id, Function<Integer[], Result> function, Integer... parameters) {
        this.id = id;
        this.function = function;
        this.parameters = parameters;
    }

    public int getId() {
        return id;
    }

    public Result execute() {
        return function.apply(parameters);
    }
}
