import java.util.*;
import java.util.concurrent.*;

class Journal {

    private final Map<String, List<List<Integer>>> data = new LinkedHashMap<>();
    private final List<String> groups  = List.of("Group-1", "Group-2", "Group-3");
    private final int studentsPerGroup = 10;

    public Journal() {
        for (String g : groups) {
            List<List<Integer>> students = new ArrayList<>();
            for (int i = 0; i < studentsPerGroup; i++) {
                students.add(new ArrayList<>());
            }
            data.put(g, students);
        }
    }

    // Синхронізований метод
    public synchronized void addGrade(String group, int student, int grade) {
        data.get(group).get(student).add(grade);
    }

    public synchronized void printSummary() {
        System.out.println("\n========== Підсумок журналу ==========");
        for (String group : groups) {
            System.out.println("\n" + group + ":");
            List<List<Integer>> students = data.get(group);
            for (int i = 0; i < students.size(); i++) {
                List<Integer> grades = students.get(i);
                double avg = grades.stream().mapToInt(x -> x).average().orElse(0);
                System.out.printf("  Студент %2d: оцінки=%s  середня=%.1f%n",
                        i + 1, grades, avg);
            }
        }
    }

    public List<String> getGroups() { return groups; }
    public int getStudentsPerGroup() { return studentsPerGroup; }
}

// Викладач виставляє оцінки одній групі за тиждень ──
class GradingTask implements Callable<String> {

    private final Journal journal;
    private final String  group;
    private final String  teacher;
    private final int     week;
    private final Random  random = new Random();

    public GradingTask(Journal journal, String group, String teacher, int week) {
        this.journal = journal;
        this.group   = group;
        this.teacher = teacher;
        this.week    = week;
    }

    @Override
    public String call() throws Exception {
        int count = journal.getStudentsPerGroup();
        for (int student = 0; student < count; student++) {
            int grade = 50 + random.nextInt(51); // оцінка 50–100
            journal.addGrade(group, student, grade);
            Thread.sleep(10); // імітація часу на виставлення оцінки
        }
        return teacher + " виставив оцінки для " + group + " (тиждень " + week + ")";
    }
}

public class GradeJournal {

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        Journal journal = new Journal();

        // Пул: 4 потоки — лектор + 3 асистенти
        ExecutorService pool = Executors.newFixedThreadPool(4);

        String[] teachers = {"Лектор", "Асистент-1", "Асистент-2", "Асистент-3"};
        int weeks = 4;

        System.out.println("Початок виставлення оцінок...\n");

        for (int week = 1; week <= weeks; week++) {
            System.out.println("--- Тиждень " + week + " ---");

            // Кожен викладач отримує одну групу на тиждень
            List<Callable<String>> tasks = new ArrayList<>();
            List<String> groups = journal.getGroups();

            for (int t = 0; t < teachers.length; t++) {
                // Лектор і асистенти розподіляють групи по колу
                String group = groups.get(t % groups.size());
                tasks.add(new GradingTask(journal, group, teachers[t], week));
            }

            // Запуск всіх задач тижня і очікування результатів
            List<Future<String>> results = pool.invokeAll(tasks);

            for (Future<String> result : results) {
                System.out.println("  " + result.get());
            }
        }

        // Завершення пулу
        pool.shutdown();
        if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
            pool.shutdownNow();
        }

        // Вивід підсумкового журналу
        journal.printSummary();
    }
}
