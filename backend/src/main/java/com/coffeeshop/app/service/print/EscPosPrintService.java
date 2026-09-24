package com.coffeeshop.app.service.print;

import com.coffeeshop.app.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * ESC/POS thermal printer integration.
 *
 * Connects to a network-attached thermal printer via TCP socket and sends
 * the formatted receipt using ESC/POS commands.
 *
 * Configuration (application.yml):
 *   print.printer.host  - printer IP/hostname (default: localhost)
 *   print.printer.port  - printer port (default: 9100)
 *   print.printer.enabled - whether printing is active (default: false)
 */
@Service
public class EscPosPrintService implements PrintService {

    private static final Logger log = LoggerFactory.getLogger(EscPosPrintService.class);

    // ESC/POS commands
    private static final byte[] ESC_INIT = {0x1B, 0x40};           // Initialize printer
    private static final byte[] ESC_ALIGN_CENTER = {0x1B, 0x61, 0x01}; // Center align
    private static final byte[] ESC_ALIGN_LEFT = {0x1B, 0x61, 0x00};   // Left align
    private static final byte[] ESC_BOLD_ON = {0x1B, 0x45, 0x01};      // Bold on
    private static final byte[] ESC_BOLD_OFF = {0x1B, 0x45, 0x00};     // Bold off
    private static final byte[] ESC_FEED_CUT = {0x1D, 0x56, 0x41, 0x00}; // Feed and cut

    @Value("${print.printer.host:localhost}")
    private String printerHost;

    @Value("${print.printer.port:9100}")
    private int printerPort;

    @Value("${print.printer.enabled:false}")
    private boolean printerEnabled;

    @Value("${print.printer.timeout-ms:5000}")
    private int timeoutMs;

    private final ReceiptFormatter receiptFormatter;

    public EscPosPrintService() {
        this.receiptFormatter = new ReceiptFormatter();
    }

    public EscPosPrintService(String printerHost, int printerPort, boolean printerEnabled,
                               int timeoutMs) {
        this.printerHost = printerHost;
        this.printerPort = printerPort;
        this.printerEnabled = printerEnabled;
        this.timeoutMs = timeoutMs;
        this.receiptFormatter = new ReceiptFormatter();
    }

    @Override
    public void printReceipt(Order order) {
        if (!printerEnabled) {
            log.info("Printer disabled. Skipping print for order #{}", order.getId());
            logReceiptToConsole(order);
            return;
        }

        String receiptText = receiptFormatter.format(order);
        sendToPrinter(receiptText, order.getId());
    }

    @Override
    public boolean isPrinterAvailable() {
        if (!printerEnabled) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(printerHost, printerPort), timeoutMs);
            return true;
        } catch (IOException e) {
            log.warn("Printer not available at {}:{}: {}", printerHost, printerPort, e.getMessage());
            return false;
        }
    }

    private void sendToPrinter(String receiptText, Long orderId) {
        log.info("Sending receipt for order #{} to printer {}:{}", orderId, printerHost, printerPort);
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(printerHost, printerPort), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            try (OutputStream out = socket.getOutputStream()) {
                out.write(ESC_INIT);
                out.write(ESC_ALIGN_LEFT);
                out.write(ESC_BOLD_OFF);
                out.write(receiptText.getBytes(StandardCharsets.UTF_8));
                out.write(ESC_FEED_CUT);
                out.flush();
            }
            log.info("Receipt for order #{} printed successfully", orderId);
        } catch (IOException e) {
            throw new PrintException("Failed to print receipt for order #" + orderId, e);
        }
    }

    private void logReceiptToConsole(Order order) {
        String receipt = receiptFormatter.format(order);
        log.debug("=== RECEIPT (printer disabled) ===\n{}\n=== END RECEIPT ===", receipt);
    }

    // Exposed for testing
    ReceiptFormatter getReceiptFormatter() {
        return receiptFormatter;
    }
}
