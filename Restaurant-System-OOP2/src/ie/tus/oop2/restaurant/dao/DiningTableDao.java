package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.model.DiningTable;

import java.util.List;

public interface DiningTableDao {

    DiningTable findById(int tableId);

    List<DiningTable> findAll();

    DiningTable insert(DiningTable table);

    boolean update(DiningTable table);

    boolean delete(int tableId);
}