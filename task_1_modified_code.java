import java.util.concurrent.locks.*;

// Клас Bank з трьома варіантами синхронізації
class SynchBank {

    public static final int NTEST = 10000;
    private final int[] accounts;
    private long ntransacts = 0;

    // ReentrantLock
    private final ReentrantLock lock = new ReentrantLock();

    public SynchBank(int n, int initialBalance) {
        accounts = new int[n];
        for (int i = 0; i < accounts.length; i++) {
            accounts[i] = initialBalance;
        }
        ntransacts = 0;
    }

    // synchronized method
    public synchronized void transferSyncMethod(int from, int to, int amount)
            throws InterruptedException {
        accounts[from] -= amount;
        accounts[to]   += amount;
        ntransacts++;
        if (ntransacts % NTEST == 0) {
            test("Sync method");
        }
    }

    //synchronized block
    public void transferSyncBlock(int from, int to, int amount)
            throws InterruptedException {
        synchronized (this) {
            accounts[from] -= amount;
            accounts[to]   += amount;
            ntransacts++;
            if (ntransacts % NTEST == 0) {
                test("Sync block");
            }
        }
    }

    // ReentrantLock
    public void transferLock(int from, int to, int amount)
            throws InterruptedException {
        lock.lock();
        try {
            accounts[from] -= amount;
            accounts[to]   += amount;
            ntransacts++;
            if (ntransacts % NTEST == 0) {
                test("ReentrantLock");
            }
        } finally {
            lock.unlock();
        }
    }

    public void test(String label) {
        int sum = 0;
        for (int i = 0; i < accounts.length; i++) {
            sum += accounts[i];
        }
        System.out.println("[" + label + "]  Transactions: " + ntransacts
                + "  Sum: " + sum);
    }

    public int size() {
        return accounts.length;
    }
}

// Потік переказів
class SynchTransferThread extends Thread {

    private SynchBank bank;
    private int fromAccount;
    private int maxAmount;
    private int variant; // 1, 2 або 3
    private static final int REPS = 1000;

    public SynchTransferThread(SynchBank b, int from, int max, int variant) {
        bank        = b;
        fromAccount = from;
        maxAmount   = max;
        this.variant = variant;
    }

    @Override
    public void run() {
        try {
            while (!interrupted()) {
                for (int i = 0; i < REPS; i++) {
                    int toAccount = (int) (bank.size() * Math.random());
                    int amount    = (int) (maxAmount * Math.random() / REPS);
                    if (variant == 1) {
                        bank.transferSyncMethod(fromAccount, toAccount, amount);
                    } else if (variant == 2) {
                        bank.transferSyncBlock(fromAccount, toAccount, amount);
                    } else {
                        bank.transferLock(fromAccount, toAccount, amount);
                    }
                    Thread.sleep(1);
                }
            }
        } catch (InterruptedException e) {
        }
    }
}

public class SynchBankTest {

    public static final int NACCOUNTS       = 10;
    public static final int INITIAL_BALANCE = 10000;
    public static final int EXPECTED_SUM    = NACCOUNTS * INITIAL_BALANCE;

    static void runVariant(int variant, String label) throws InterruptedException {
        System.out.println("\n=== Варіант " + variant + ": " + label + " ===");
        System.out.println("Очікувана сума: " + EXPECTED_SUM);

        SynchBank bank = new SynchBank(NACCOUNTS, INITIAL_BALANCE);
        Thread[] threads = new Thread[NACCOUNTS];

        for (int i = 0; i < NACCOUNTS; i++) {
            SynchTransferThread t =
                    new SynchTransferThread(bank, i, INITIAL_BALANCE, variant);
            t.setPriority(Thread.NORM_PRIORITY + i % 2);
            t.setDaemon(true);
            threads[i] = t;
            t.start();
        }

        Thread.sleep(3000); // дає попрацювати 3 секунди

        // Зупинка всіх потоків
        for (Thread t : threads) {
            t.interrupt();
        }
        for (Thread t : threads) {
            t.join(500);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        runVariant(1, "synchronized method");
        runVariant(2, "synchronized block");
        runVariant(3, "ReentrantLock");
    }
}
