package ie.tus.oop2.restaurant.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL =
            "jdbc:mysql://127.0.0.1:3306/restaurant_system_oop2?useSSL=false&serverTimezone=UTC";

    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connection;

    private DatabaseConnection() {
        // private constructor to prevent instantiation
    }

    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Database connected successfully!");
            } catch (SQLException e) {
                System.err.println("❌ Database connection failed!");
                e.printStackTrace();
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
                System.out.println("🔒 Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
