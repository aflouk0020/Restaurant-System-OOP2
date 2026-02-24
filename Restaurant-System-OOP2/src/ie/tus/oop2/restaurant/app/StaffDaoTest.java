package ie.tus.oop2.restaurant.app;

import ie.tus.oop2.restaurant.dao.StaffDao;
import ie.tus.oop2.restaurant.dao.StaffDaoImpl;
import ie.tus.oop2.restaurant.model.Staff;
import ie.tus.oop2.restaurant.model.StaffRole;


import java.util.List;

public class StaffDaoTest {

    public static void main(String[] args) {

    	StaffDao staffDao = new StaffDaoImpl();

        Staff newStaff = new Staff(
                0,
                "Alice Brown",
                StaffRole.WAITER,
                "alice@example.com",
                true,
                null
        );

        Staff saved = staffDao.insert(newStaff);
        System.out.println("Inserted: " + saved);

        List<Staff> all = staffDao.findAll();
        System.out.println("All staff: " + all);

        Staff found = staffDao.findById(saved.staffId());
        System.out.println("Found by ID: " + found);

        saved = new Staff(
                saved.staffId(),
                "Alice Brown Updated",
                StaffRole.MANAGER,
                "alice@example.com",
                true,
                saved.createdAt()
        );

        staffDao.update(saved);
        System.out.println("After update: " + staffDao.findById(saved.staffId()));

        // staffDao.delete(saved.staffId());
        // System.out.println("Deleted? check findAll(): " + staffDao.findAll());
    }
}
