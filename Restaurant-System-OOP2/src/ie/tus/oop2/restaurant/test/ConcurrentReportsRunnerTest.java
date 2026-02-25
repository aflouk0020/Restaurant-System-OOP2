package ie.tus.oop2.restaurant.test;

import ie.tus.oop2.restaurant.concurrency.ConcurrentReportsRunner;
import ie.tus.oop2.restaurant.reporting.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentReportsRunnerTest {

    @Test
    void runAllReports_shouldExecuteWithoutError() {

        ManagerReportsService reports = new ManagerReportsServiceImpl();
        ConcurrentReportsRunner runner = new ConcurrentReportsRunner(reports);

        ReportsSnapshot snapshot = runner.runAllReports();

        assertNotNull(snapshot);
        assertNotNull(snapshot.dailySales());
        assertNotNull(snapshot.topSelling());
        assertNotNull(snapshot.vegetarianPartition());
    }
}