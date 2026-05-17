import java.util.concurrent.*;

// Producer
class Producer implements Runnable {

    private BlockingQueue<Integer> drop;
    private int[] data;

    public Producer(BlockingQueue<Integer> drop, int[] data) {
        this.drop = drop;
        this.data = data;
    }

    @Override
    public void run() {
        try {
            for (int i = 0; i < data.length; i++) {
                drop.put(data[i]);
                System.out.println("Produced: " + data[i]);
            }
            drop.put(Integer.MIN_VALUE); // сигнал завершення
        } catch (InterruptedException e) {
        }
    }
}

// Consumer
class Consumer implements Runnable {

    private BlockingQueue<Integer> drop;

    public Consumer(BlockingQueue<Integer> drop) {
        this.drop = drop;
    }

    @Override
    public void run() {
        try {
            while (true) {
                int value = drop.take();
                if (value == Integer.MIN_VALUE) { // сигнал завершення
                    break;
                }
                System.out.println("Consumed: " + value);
            }
        } catch (InterruptedException e) {
        }
    }
}

// Головна програма
public class ProducerConsumer {

    static void runTest(int size) throws InterruptedException {
        System.out.println("\n=== Тест: масив розміром " + size + " ===");

        int[] data = new int[size];
        for (int i = 0; i < size; i++) {
            data[i] = i + 1;
        }

        BlockingQueue<Integer> drop = new SynchronousQueue<Integer>();

        Thread producerThread = new Thread(new Producer(drop, data));
        Thread consumerThread = new Thread(new Consumer(drop));

        producerThread.start();
        consumerThread.start();

        producerThread.join();
        consumerThread.join();

        System.out.println("Тест завершено. Передано " + size + " чисел.");
    }

    public static void main(String[] args) throws InterruptedException {
        runTest(100);
        runTest(1000);
        runTest(5000);
    }
