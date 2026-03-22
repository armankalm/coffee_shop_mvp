package com.coffeeshop.app.service.print;

import com.coffeeshop.app.domain.Order;

public interface PrintService {

    /**
     * Print a receipt for the given order.
     *
     * @param order the order to print
     * @throws PrintException if printing fails
     */
    void printReceipt(Order order);

    /**
     * Returns true if the printer is currently available.
     */
    boolean isPrinterAvailable();
}
