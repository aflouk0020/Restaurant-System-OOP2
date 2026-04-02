package ie.tus.oop2.restaurant.service;

import java.io.IOException;
import ie.tus.oop2.restaurant.service.SettingsService;
import ie.tus.oop2.restaurant.service.SettingsServiceImpl;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import ie.tus.oop2.restaurant.dao.OrderLineDao;
import ie.tus.oop2.restaurant.dao.OrderLineDaoImpl;
import ie.tus.oop2.restaurant.dao.PaymentDao;
import ie.tus.oop2.restaurant.dao.PaymentDaoImpl;
import ie.tus.oop2.restaurant.dao.ReceiptDao;
import ie.tus.oop2.restaurant.dao.ReceiptDaoImpl;
import ie.tus.oop2.restaurant.model.OrderLine;
import ie.tus.oop2.restaurant.model.OrderLineStatus;
import ie.tus.oop2.restaurant.model.Payment;
import ie.tus.oop2.restaurant.model.Receipt;
import ie.tus.oop2.restaurant.model.payment.CardCtx;
import ie.tus.oop2.restaurant.model.payment.CashCtx;
import ie.tus.oop2.restaurant.model.payment.PaymentValidationContext;
import ie.tus.oop2.restaurant.model.payment.PaymentValidator;
import ie.tus.oop2.restaurant.model.payment.VoucherCtx;
import ie.tus.oop2.restaurant.util.MoneyUtil;

public class ReceiptServiceImpl implements ReceiptService {

	private final SettingsService settingsService;


    // receipt_<orderId>_<timestamp>.txt
    private static final DateTimeFormatter FILE_TS =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ReceiptDao receiptDao;
    private final PaymentDao paymentDao;
    private final OrderLineDao orderLineDao;

    public ReceiptServiceImpl() {
        this.receiptDao = new ReceiptDaoImpl();
        this.paymentDao = new PaymentDaoImpl();
        this.orderLineDao = new OrderLineDaoImpl();
        this.settingsService = new SettingsServiceImpl();
    }
    
    public ReceiptServiceImpl(ReceiptDao receiptDao, PaymentDao paymentDao, OrderLineDao orderLineDao) {
        this.receiptDao = receiptDao;
        this.paymentDao = paymentDao;
        this.orderLineDao = orderLineDao;
        this.settingsService = new SettingsServiceImpl();
    }

    @Override
    public Receipt generateReceiptForOrder(long orderId, Path exportDir) {

        Payment payment = paymentDao.findByOrderId(orderId);
        if (payment == null) {
            throw new IllegalStateException("Cannot generate receipt: no payment exists for order " + orderId);
        }
        Receipt existing = receiptDao.findByOrderId(orderId);
        if (existing != null) {
            if (exportDir != null) {
                exportReceiptToFile(existing, payment, exportDir);
            }
            return existing;
        }
        List<OrderLine> lines = orderLineDao.findByOrderId(orderId);
        if (lines == null || lines.isEmpty()) {
            throw new IllegalStateException("Cannot generate receipt: order has no lines (orderId=" + orderId + ")");
        }

        BigDecimal subtotal = MoneyUtil.scale(
                lines.stream()
                        .filter(l -> l.lineStatus() != OrderLineStatus.CANCELLED)
                        .map(l -> l.unitPriceSnapshot().multiply(BigDecimal.valueOf(l.quantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
        );

        if (subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Cannot generate receipt: subtotal <= 0 for order " + orderId);
        }

        BigDecimal taxRate = settingsService.load().taxRate();
        BigDecimal tax = MoneyUtil.scale(subtotal.multiply(taxRate));
        BigDecimal total = MoneyUtil.scale(subtotal.add(tax));
        BigDecimal paid = MoneyUtil.scale(payment.amount());

        // ✅ Advanced: switch expression + sealed types + pattern matching switch
        validatePayment(payment, paid, total);

        Receipt input = new Receipt(
                0,
                orderId,
                payment.paymentId(),
                subtotal,
                tax,
                total,
                LocalDateTime.now().withNano(0)
        );

        Receipt saved = receiptDao.insert(input);

        if (exportDir != null) {
            exportReceiptToFile(saved, payment, exportDir);
        }

        return saved;
    }

    private void validatePayment(Payment payment, BigDecimal paid, BigDecimal total) {

        // ✅ switch expression (enum → sealed context)
        PaymentValidationContext ctx = switch (payment.paymentType()) {
            case CASH -> new CashCtx(paid, total);
            case CARD -> new CardCtx(paid, total, payment.cardLast4());
            case VOUCHER -> new VoucherCtx(paid, total, payment.voucherCode());
        };

        // ✅ pattern matching switch happens inside validator
        PaymentValidator.validate(ctx);
    }



    private void exportReceiptToFile(Receipt receipt, Payment payment, Path exportDir) {
        try {
            Files.createDirectories(exportDir);

            String timestamp = receipt.generatedAt().format(FILE_TS);
            String fileName = "receipt_" + receipt.orderId() + "_" + timestamp + ".txt";

            Path out = exportDir.resolve(fileName);
            Files.writeString(out, buildReceiptText(receipt, payment));

        } catch (IOException e) {
            throw new RuntimeException("Failed to export receipt file: " + e.getMessage(), e);
        }
    }
 

    private String buildReceiptText(Receipt r, Payment p) {
        return """
                ============================
                RESTAURANT RECEIPT
                ============================
                Receipt ID: %d
                Order ID:   %d
                Payment ID: %d
                Generated:  %s

                Subtotal:   %s
                Tax:        %s
                Total:      %s

                Payment Type: %s
                Paid Amount:  %s %s
                ============================
                """.formatted(
                r.receiptId(),
                r.orderId(),
                r.paymentId(),
                r.generatedAt(),
                MoneyUtil.formatPlain(r.subtotal()),
                MoneyUtil.formatPlain(r.tax()),
                MoneyUtil.formatPlain(r.total()),
                p.paymentType(),
                MoneyUtil.formatPlain(p.amount()),
                p.currency()
        );
    }
}