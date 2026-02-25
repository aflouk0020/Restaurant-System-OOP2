package ie.tus.oop2.restaurant.dao;

import ie.tus.oop2.restaurant.model.Receipt;

import java.util.List;

public interface ReceiptDao {

    Receipt findById(long receiptId);

    Receipt findByOrderId(long orderId);     // UNIQUE(order_id)

    Receipt findByPaymentId(long paymentId); // UNIQUE(payment_id)

    List<Receipt> findAll();

    Receipt insert(Receipt receipt);

    boolean delete(long receiptId);
}