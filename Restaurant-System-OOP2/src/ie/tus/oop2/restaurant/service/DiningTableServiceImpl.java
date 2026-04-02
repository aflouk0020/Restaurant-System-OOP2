package ie.tus.oop2.restaurant.service;

import ie.tus.oop2.restaurant.dao.DiningTableDao;
import ie.tus.oop2.restaurant.dao.DiningTableDaoImpl;
import ie.tus.oop2.restaurant.model.DiningTable;

import java.util.List;

public class DiningTableServiceImpl implements DiningTableService {

    private final DiningTableDao diningTableDao = new DiningTableDaoImpl();

    @Override
    public List<DiningTable> listTables() {
        return diningTableDao.findAll();
    }
}