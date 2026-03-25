package ie.tus.oop2.restaurant.concurrency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

import ie.tus.oop2.restaurant.reporting.ManagerReportsService;
import ie.tus.oop2.restaurant.reporting.ReportsSnapshot;

public class ConcurrentReportsRunner {

    private final ManagerReportsService reportsService;

    public ConcurrentReportsRunner(ManagerReportsService reportsService) {
        this.reportsService = reportsService;
    }

    /**
     * Runs all reports in parallel using ExecutorService + Callable + Future.
     */
    public ReportsSnapshot runAllReports() {

        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            Callable<LinkedHashMap<LocalDate, BigDecimal>> dailyTask =
                    reportsService::dailySalesTotals;

            Callable<LinkedHashMap<String, Long>> topTask =
                    () -> reportsService.topSellingItems(10);

            Callable<Map<Boolean, BigDecimal>> partitionTask =
                    reportsService::partitionSalesByVegetarian;

            Future<LinkedHashMap<LocalDate, BigDecimal>> fDaily = executor.submit(dailyTask);
            Future<LinkedHashMap<String, Long>> fTop = executor.submit(topTask);
            Future<Map<Boolean, BigDecimal>> fPartition = executor.submit(partitionTask);

            return new ReportsSnapshot(
                    fDaily.get(),
                    fTop.get(),
                    fPartition.get()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Report execution interrupted", e);

        } catch (ExecutionException e) {
            throw new RuntimeException("Report execution failed", e.getCause());

        } finally {
            executor.shutdown();
        }
    }
}