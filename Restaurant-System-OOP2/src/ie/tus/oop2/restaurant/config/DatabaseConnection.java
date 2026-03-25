package ie.tus.oop2.restaurant.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Simple singleton-style JDBC connection helper.
 *
 * Note:
 * - DAOs must NOT close this shared connection.
 * - Close it once on application shutdown.
 */
public final class DatabaseConnection {

    private static final String URL =
            "jdbc:mysql://127.0.0.1:3306/restaurant_system_oop2?useSSL=false&serverTimezone=UTC";

    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connection;

    private DatabaseConnection() {
        // prevent instantiation
    }

    /**
     * Returns a live DB connection. If the connection is null OR has been closed,
     * it will be created again.
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Database connected successfully!");
            }
            return connection;
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed!");
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes the shared connection (call once when the app exits).
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
                System.out.println("🔒 Database connection closed.");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
