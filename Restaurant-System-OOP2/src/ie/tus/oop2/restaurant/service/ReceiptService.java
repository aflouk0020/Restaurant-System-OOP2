package ie.tus.oop2.restaurant.service;

import ie.tus.oop2.restaurant.model.Receipt;

import java.nio.file.Path;

public interface ReceiptService {

    /**
     * Generates a receipt for an order:
     * - reads order lines
     * - calculates subtotal
     * - applies tax
     * - checks payment exists and matches total
     * - inserts receipt
     * - optionally exports to file (if exportDir != null)
     */
    Receipt generateReceiptForOrder(long orderId, Path exportDir);
}