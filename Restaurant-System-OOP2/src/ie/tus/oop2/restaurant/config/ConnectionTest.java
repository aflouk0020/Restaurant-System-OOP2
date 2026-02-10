package ie.tus.oop2.restaurant.config;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import java.sql.Connection;

public class ConnectionTest {

    public static void main(String[] args) {

        System.out.println("Testing database connection...");

        Connection conn = DatabaseConnection.getConnection();

        if (conn != null) {
            System.out.println("Connection works ✅");
        } else {
            System.out.println("Connection failed ❌");
        }

        DatabaseConnection.closeConnection();
    }
}
