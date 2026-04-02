package ie.tus.oop2.restaurant.service;

import ie.tus.oop2.restaurant.model.Payment;
import ie.tus.oop2.restaurant.model.PaymentType;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    Payment pay(long orderId,
                PaymentType type,
                BigDecimal amount,
                String currency,
                String cardLast4,
                String voucherCode);

    Payment findByOrderId(long orderId);

    List<Payment> listPayments();
}