import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

public class Main {
    public static void main(String[] args) {
        final int NUM_TASKS = 30; // Всього задач
        final int NUM_ADD_THREADS = 2; // Потоки, що додають задачі
        final int PER_THREAD = NUM_TASKS / NUM_ADD_THREADS; // Скільки задач на 1 потік

        ThreadPool<Integer> pool = new ThreadPool<>(6);
        pool.initialize();

        List<Thread> addThreads = new ArrayList<>();
        long start = System.nanoTime();

        for (int i = 0; i < NUM_ADD_THREADS; i++) {
            final int threadIndex = i;
            Thread thread = new Thread(() -> {
                for (int j = threadIndex * PER_THREAD; j < (threadIndex + 1) * PER_THREAD; j++) {
                    int taskId = pool.addTask(Main::task, j, j + 1);
                    if (taskId != -1) {
                        System.out.println("Task added with ID: " + taskId);
                    }
                }
            });
            addThreads.add(thread);
            thread.start();
        }

        for (Thread thread : addThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        pool.terminate();
        long end = System.nanoTime();

        int missingTasks = pool.getTaskQueue().getMissingTasks();
        long fullQueueTime = pool.getTaskQueue().getFullTime();
        System.out.println("Missing tasks: " + missingTasks);
        System.out.println("Full queue duration: " + (fullQueueTime > 0 ? (System.currentTimeMillis() - fullQueueTime) + " ms" : "Queue never full"));
        System.out.println("Total time: " + ((end - start) / 1e6) + " ms");
    }

    public static Integer task(Integer[] params) {
        try {
            Thread.sleep(5000 + new Random().nextInt(6000)); // 5-10 сек
        } catch (InterruptedException e) {
            System.out.println("Task interrupted.");
            return null;
        }
        return params[0] + params[1];
    }
}
