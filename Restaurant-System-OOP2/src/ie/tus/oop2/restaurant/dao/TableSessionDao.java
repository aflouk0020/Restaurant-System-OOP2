package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.model.TableSession;

import java.util.List;

public interface TableSessionDao {

    TableSession findById(long sessionId);

    List<TableSession> findAll();

    TableSession insert(TableSession session);

    boolean update(TableSession session);

    boolean delete(long sessionId);

    // Useful for business rules / service
    TableSession findOpenByTableId(int tableId);

    TableSession findByReservationId(long reservationId);
}