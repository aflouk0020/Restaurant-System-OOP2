package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.model.MenuCategory;
import ie.tus.oop2.restaurant.model.MenuItem;

import java.util.List;

public interface MenuItemDao {

    MenuItem findById(long menuItemId);

    List<MenuItem> findAll();

    List<MenuItem> findByCategory(MenuCategory category);

    List<MenuItem> findAvailableOnly();

    MenuItem insert(MenuItem item);

    boolean update(MenuItem item);

    boolean delete(long menuItemId);
}