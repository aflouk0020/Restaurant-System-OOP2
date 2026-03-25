package ie.tus.oop2.restaurant.service;

import ie.tus.oop2.restaurant.model.TableSession;

public interface TableSessionService {

    TableSession openSession(int tableId, Long openedByStaffId);

    TableSession closeSession(long sessionId);

    TableSession seatReservation(long reservationId, Long openedByStaffId);
}