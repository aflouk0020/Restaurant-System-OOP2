package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.model.Reservation;

import java.util.List;

public interface ReservationDao {
    Reservation findById(long reservationId);

    List<Reservation> findAll();

    Reservation insert(Reservation reservation);

    boolean update(Reservation reservation);

    boolean delete(long reservationId);
}