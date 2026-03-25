package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.model.Staff;
import java.util.List;

public interface StaffDao {
    Staff findById(long staffId);
    List<Staff> findAll();
    Staff insert(Staff staff);
    boolean update(Staff staff);
    boolean delete(long staffId);
}