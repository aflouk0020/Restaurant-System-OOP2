package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.config.DatabaseConnection;
import ie.tus.oop2.restaurant.model.MenuCategory;
import ie.tus.oop2.restaurant.model.MenuItem;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC DAO implementation for MENU_ITEM table.
 *
 * Table: menu_item
 * Columns: menu_item_id, name, category, price, vegetarian, available, created_at
 *
 * Important: DO NOT close the shared connection from DatabaseConnection in DAO methods.
 */
public class MenuItemDaoImpl implements MenuItemDao {

    private static final String COL_ID = "menu_item_id";
    private static final String COL_NAME = "name";
    private static final String COL_CATEGORY = "category";
    private static final String COL_PRICE = "price";
    private static final String COL_VEGETARIAN = "vegetarian";
    private static final String COL_AVAILABLE = "available";
    private static final String COL_CREATED_AT = "created_at";

    @Override
    public MenuItem findById(long menuItemId) {
        String sql = """
                SELECT menu_item_id, name, category, price, vegetarian, available, created_at
                FROM menu_item
                WHERE menu_item_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, menuItemId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find menu item by id: " + menuItemId, e);
        }
    }

    @Override
    public List<MenuItem> findAll() {
        String sql = """
                SELECT menu_item_id, name, category, price, vegetarian, available, created_at
                FROM menu_item
                ORDER BY menu_item_id
                """;

        Connection c = DatabaseConnection.getConnection();
        List<MenuItem> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to findAll menu items", e);
        }
    }

    @Override
    public List<MenuItem> findByCategory(MenuCategory category) {
        String sql = """
                SELECT menu_item_id, name, category, price, vegetarian, available, created_at
                FROM menu_item
                WHERE category = ?
                ORDER BY menu_item_id
                """;

        Connection c = DatabaseConnection.getConnection();
        List<MenuItem> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, category.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find menu items by category: " + category, e);
        }
    }

    @Override
    public List<MenuItem> findAvailableOnly() {
        String sql = """
                SELECT menu_item_id, name, category, price, vegetarian, available, created_at
                FROM menu_item
                WHERE available = TRUE
                ORDER BY menu_item_id
                """;

        Connection c = DatabaseConnection.getConnection();
        List<MenuItem> list = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find available menu items", e);
        }
    }

    @Override
    public MenuItem insert(MenuItem item) {
        // created_at comes from DB default CURRENT_TIMESTAMP
        String sql = """
                INSERT INTO menu_item (name, category, price, vegetarian, available)
                VALUES (?, ?, ?, ?, ?)
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.name());
            ps.setString(2, item.category().name());
            ps.setBigDecimal(3, item.price());
            ps.setBoolean(4, item.vegetarian());
            ps.setBoolean(5, item.available());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new SQLException("Insert failed, no rows affected.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long newId = keys.getLong(1);
                    return findById(newId);
                }
                throw new SQLException("Insert failed, no generated key returned.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert menu item", e);
        }
    }

    @Override
    public boolean update(MenuItem item) {
        // Do NOT update created_at
        String sql = """
                UPDATE menu_item
                SET name = ?, category = ?, price = ?, vegetarian = ?, available = ?
                WHERE menu_item_id = ?
                """;

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, item.name());
            ps.setString(2, item.category().name());
            ps.setBigDecimal(3, item.price());
            ps.setBoolean(4, item.vegetarian());
            ps.setBoolean(5, item.available());
            ps.setLong(6, item.menuItemId());

            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update menu item id: " + item.menuItemId(), e);
        }
    }

    @Override
    public boolean delete(long menuItemId) {
        String sql = "DELETE FROM menu_item WHERE menu_item_id = ?";

        Connection c = DatabaseConnection.getConnection();

        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, menuItemId);
            int affected = ps.executeUpdate();
            return affected > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete menu item id: " + menuItemId, e);
        }
    }

    // -------------------------
    // Row mapper
    // -------------------------
    private MenuItem mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong(COL_ID);
        String name = rs.getString(COL_NAME);

        String categoryStr = rs.getString(COL_CATEGORY);
        MenuCategory category = MenuCategory.valueOf(categoryStr);

        BigDecimal price = rs.getBigDecimal(COL_PRICE);
        boolean vegetarian = rs.getBoolean(COL_VEGETARIAN);
        boolean available = rs.getBoolean(COL_AVAILABLE);

        Timestamp ts = rs.getTimestamp(COL_CREATED_AT);
        LocalDateTime createdAt = (ts == null) ? null : ts.toLocalDateTime();

        return new MenuItem(id, name, category, price, vegetarian, available, createdAt);
    }
}