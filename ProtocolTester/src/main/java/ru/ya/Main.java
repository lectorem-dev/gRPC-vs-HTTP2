package ru.ya;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Многопроходный тестировщик протоколов HTTP/gRPC
 * - выполняет RUNS прогонов
 * - каждый прогон делает ITERATIONS запросов с THREAD_COUNT потоками
 * - выводит прогресс выполнения в виде шкалы и сводные статистики
 */
public class Main {
    // URL отправителя
    private static final String SENDER_URL = "http://localhost:8000/api/sender/send?protocol=";

    // ----- НАСТРОЙКИ -----
    private static final String PROTOCOL = "grpc";       // grpc или http
    private static final int RUNS = 500;                 // количество полных прогонов
    private static final int ITERATIONS = 500;           // запросов на прогон
    private static final int THREAD_COUNT = 50;          // потоков на прогон
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    // Настройка генерации JSON
    private static final int REQUEST_BLOCK_COUNT = 50;   // сколько блоков "блох" в массиве

    private static final String REQUEST_JSON = generateRequestJson(REQUEST_BLOCK_COUNT);

    public static void main(String[] args) throws Exception {
        long scriptStart = System.nanoTime();
        printConfig();

        // Создаём директорию для вывода (если нужно)
        try {
            Files.createDirectories(Paths.get("output"));
        } catch (Exception ignored) {}

        // Хранилище метрик всех прогонов
        List<Double> runAvgMs = new ArrayList<>();
        List<Double> runMedianMs = new ArrayList<>();
        List<Double> runP90Ms = new ArrayList<>();
        List<Double> runP95Ms = new ArrayList<>();
        List<Double> runMaxMs = new ArrayList<>();
        List<Double> runThroughput = new ArrayList<>();
        List<Long> allDurationsNs = new ArrayList<>();

        AtomicInteger globalProgress = new AtomicInteger(0);
        int totalRequests = RUNS * ITERATIONS;

        for (int run = 1; run <= RUNS; run++) {
            // System.out.printf("%n=== RUN %d/%d ===%n", run, RUNS);

            // Запуск одного прогона
            RunResult result = runSingleBatch(ITERATIONS, THREAD_COUNT, run, globalProgress, totalRequests);

            // Сбор метрик для усреднения
            runAvgMs.add(nanosToMillisDouble(avg(result.durationsNs)));
            runMedianMs.add(nanosToMillisDouble(median(result.sortedDurationsNs)));
            runP90Ms.add(nanosToMillisDouble(percentile(result.sortedDurationsNs, 90)));
            runP95Ms.add(nanosToMillisDouble(percentile(result.sortedDurationsNs, 95)));
            runMaxMs.add(nanosToMillisDouble(Collections.max(result.sortedDurationsNs)));
            runThroughput.add(result.throughput);

            allDurationsNs.addAll(result.durationsNs);

            Thread.sleep(500); // короткая пауза между прогонами
        }

        System.out.println(); // перенос строки после прогресс-бара

        // ----- MULTI-RUN SUMMARY -----
        System.out.println("\n=== MULTI-RUN SUMMARY ===");
        System.out.printf("Average per run: %s ms%n", formatDouble(mean(runAvgMs)));
        System.out.printf("Median per run: %s ms%n", formatDouble(mean(runMedianMs)));
        System.out.printf("p90 per run: %s ms%n", formatDouble(mean(runP90Ms)));
        System.out.printf("p95 per run: %s ms%n", formatDouble(mean(runP95Ms)));
        System.out.printf("Max per run: %s ms%n", formatDouble(mean(runMaxMs)));
        System.out.printf("Average throughput: %s req/s%n", formatDouble(mean(runThroughput)));

        // ----- Агрегированные статистики всех успешных запросов -----
        System.out.println("\n=== Aggregated statistics ===");
        if (!allDurationsNs.isEmpty()) {
            printStatsMs(allDurationsNs);
        } else {
            System.out.println("No successful requests to analyze.");
        }

        long scriptEnd = System.nanoTime();
        double scriptSec = (scriptEnd - scriptStart) / 1_000_000_000.0;
        String GREEN = "\u001B[32m";
        String RESET = "\u001B[0m";
        System.out.println(GREEN + "Done. Total runtime: " + formatDouble(scriptSec) + " s" + RESET);
    }

    // ----- Генерация JSON -----
    private static String generateFleasBlock() {
        return """
        {
            "n": 4,
            "m": 4,
            "feederRow": 0,
            "feederCol": 0,
            "fleasCount": 16,
            "fleas": [
              {"row":0,"col":0},{"row":0,"col":1},{"row":0,"col":2},{"row":0,"col":3},
              {"row":1,"col":0},{"row":1,"col":1},{"row":1,"col":2},{"row":1,"col":3},
              {"row":2,"col":0},{"row":2,"col":1},{"row":2,"col":2},{"row":2,"col":3},
              {"row":3,"col":0},{"row":3,"col":1},{"row":3,"col":2},{"row":3,"col":3}
            ]
        }
        """;
    }

    private static String generateRequestJson(int blockCount) {
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < blockCount; i++) {
            sb.append(generateFleasBlock());
            if (i < blockCount - 1) sb.append(",\n");
        }
        sb.append("\n]");
        return sb.toString();
    }

    // ----- Выполнение одного прогона -----
    private static RunResult runSingleBatch(int iterations, int threads, int run, AtomicInteger globalProgress, int totalRequests) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CompletionService<Long> completion = new ExecutorCompletionService<>(executor);
        AtomicInteger failures = new AtomicInteger(0);
        List<Long> durationsNs = Collections.synchronizedList(new ArrayList<>(iterations));

        long submitStart = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            completion.submit(() -> {
                try {
                    long duration = singlePostRoundtrip(SENDER_URL + PROTOCOL, REQUEST_JSON);
                    durationsNs.add(duration);
                    int currentGlobal = globalProgress.incrementAndGet();
                    printProgress(currentGlobal, totalRequests);
                    return duration;
                } catch (Exception e) {
                    failures.incrementAndGet();
                    globalProgress.incrementAndGet();
                    return -1L;
                }
            });
        }

        // Ожидание завершения всех задач
        for (int i = 0; i < iterations; i++) {
            try {
                Future<Long> f = completion.take();
                f.get();
            } catch (ExecutionException ee) {
                failures.incrementAndGet();
            }
        }
        long submitEnd = System.nanoTime();
        executor.shutdown();

        int successes = durationsNs.size();
        double wallSec = (submitEnd - submitStart) / 1_000_000_000.0;
        double throughput = wallSec > 0 ? successes / wallSec : 0.0;

        List<Long> sorted = new ArrayList<>(durationsNs);
        Collections.sort(sorted);

        // System.out.println(); // перенос строки после прогресс-бара - убрано

        return new RunResult(iterations, successes, failures.get(), durationsNs, sorted, wallSec, throughput);
    }

    // ----- Прогресс-бар -----
    private static void printProgress(int current, int total) {
        int width = 30; // ширина шкалы
        int done = (int)((current / (double)total) * width);
        int remaining = width - done;
        StringBuilder bar = new StringBuilder("[");
        bar.append("█".repeat(done));
        bar.append(".".repeat(Math.max(0, remaining)));
        bar.append("] ");
        bar.append(current).append("/").append(total);
        // обновляем строку на месте
        System.out.print("\r" + bar);
        // только в конце делаем перевод строки
        if (current == total) {
            System.out.println();
        }
    }

    // ----- HTTP POST -----
    private static long singlePostRoundtrip(String targetUrl, String json) throws Exception {
        URL url = new URL(targetUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        byte[] payload = json.getBytes(StandardCharsets.UTF_8);

        long start = System.nanoTime();
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
            os.flush();
        }

        int code = conn.getResponseCode();
        if (code != 200 && code != 201 && code != 204) {
            String err = readStream(conn.getErrorStream());
            throw new RuntimeException("HTTP " + code + (err == null ? "" : " : " + err));
        }

        readStream(conn.getInputStream());
        long end = System.nanoTime();
        conn.disconnect();
        return end - start;
    }

    private static String readStream(InputStream in) {
        if (in == null) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ----- Статистика -----
    private static void printStatsMs(List<Long> durationsNs) {
        List<Long> copy = new ArrayList<>(durationsNs);
        Collections.sort(copy);
        int n = copy.size();
        double avgNs = avg(copy);
        long minNs = n > 0 ? copy.get(0) : 0;
        long maxNs = n > 0 ? copy.get(n - 1) : 0;
        double medianNs = median(copy);
        double p90Ns = percentile(copy, 90);
        double p95Ns = percentile(copy, 95);
        double stddevMs = stddev(copy) / 1_000_000.0;

        DecimalFormat df = new DecimalFormat("#,##0.000");

        System.out.println("Per-request round-trip (milliseconds):");
        System.out.printf("count: %d%n", n);
        System.out.printf("min: %s ms%n", df.format(nanosToMillis(minNs)));
        System.out.printf("avg: %s ms%n", df.format(nanosToMillisDouble(avgNs)));
        System.out.printf("median: %s ms%n", df.format(nanosToMillisDouble(medianNs)));
        System.out.printf("p90: %s ms%n", df.format(nanosToMillisDouble(p90Ns)));
        System.out.printf("p95: %s ms%n", df.format(nanosToMillisDouble(p95Ns)));
        System.out.printf("max: %s ms%n", df.format(nanosToMillis(maxNs)));
        System.out.printf("stddev: %s ms%n\n", df.format(stddevMs));
    }

    private static long percentile(List<Long> sorted, double p) {
        if (sorted.isEmpty()) return 0;
        int n = sorted.size();
        double idx = (p / 100.0) * (n - 1);
        int lo = (int) Math.floor(idx);
        int hi = (int) Math.ceil(idx);
        if (lo == hi) return sorted.get(lo);
        double w = idx - lo;
        return Math.round(sorted.get(lo) * (1 - w) + sorted.get(hi) * w);
    }

    private static double median(List<Long> sorted) {
        int n = sorted.size();
        if (n == 0) return 0;
        if (n % 2 == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private static double avg(List<Long> durations) {
        if (durations.isEmpty()) return 0.0;
        double sum = 0;
        for (Long v : durations) sum += v;
        return sum / durations.size();
    }

    private static double stddev(List<Long> values) {
        int n = values.size();
        if (n <= 1) return 0.0;
        double mean = avg(values);
        double sumsq = 0;
        for (Long v : values) sumsq += (v - mean) * (v - mean);
        return Math.sqrt(sumsq / (n - 1));
    }

    private static double nanosToMillis(long ns) {
        return ns / 1_000_000.0;
    }

    private static double nanosToMillisDouble(double ns) {
        return ns / 1_000_000.0;
    }

    private static String formatDouble(double v) {
        return new DecimalFormat("#,##0.000").format(v);
    }

    private static double mean(List<Double> vals) {
        if (vals.isEmpty()) return 0.0;
        double sum = 0;
        for (Double d : vals) sum += d;
        return sum / vals.size();
    }

    // ----- Вывод конфигурации -----
    private static void printConfig() {
        System.out.println("\n\n=== CONFIGURATION ===");
        System.out.println("Protocol: " + PROTOCOL);
        System.out.println("Runs: " + RUNS);
        System.out.println("Requests/run: " + ITERATIONS);
        System.out.println("Total requests: " + (RUNS * ITERATIONS));
        System.out.println("Threads: " + THREAD_COUNT);
        System.out.println("Connect timeout: " + CONNECT_TIMEOUT_MS + " ms");
        System.out.println("Read timeout: " + READ_TIMEOUT_MS + " ms");
        System.out.println("Request blocks: " + REQUEST_BLOCK_COUNT + "\n\n");
    }

    // ----- Класс для хранения результата прогона -----
    private static class RunResult {
        final int submitted;
        final int successes;
        final int failures;
        final List<Long> durationsNs;
        final List<Long> sortedDurationsNs;
        final double wallSec;
        final double throughput;

        RunResult(int submitted, int successes, int failures, List<Long> durationsNs,
                  List<Long> sortedDurationsNs, double wallSec, double throughput) {
            this.submitted = submitted;
            this.successes = successes;
            this.failures = failures;
            this.durationsNs = durationsNs;
            this.sortedDurationsNs = sortedDurationsNs;
            this.wallSec = wallSec;
            this.throughput = throughput;
        }
    }
}