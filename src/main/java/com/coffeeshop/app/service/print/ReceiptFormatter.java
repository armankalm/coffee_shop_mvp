package com.coffeeshop.app.service.print;

import com.coffeeshop.app.domain.Order;
import com.coffeeshop.app.domain.OrderItem;
import com.coffeeshop.app.domain.Topping;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Formats an order into a printable receipt string suitable for ESC/POS printers.
 * The receipt includes shop name, order details, items with toppings, total, and a barcode line.
 */
public class ReceiptFormatter {

    private static final int LINE_WIDTH = 42;
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    public String format(Order order) {
        StringBuilder sb = new StringBuilder();

        // Header
        appendCentered(sb, "*** COFFEE SHOP ***");
        appendCentered(sb, order.getShop().getName());
        appendCentered(sb, order.getShop().getAddress());
        appendLine(sb);

        // Order info
        sb.append(String.format("%-20s%s%n", "Order #" + order.getId(),
                DATE_FMT.format(order.getCreatedAt())));
        sb.append(String.format("Customer: %s%n", maskEmail(order.getUser().getEmail())));
        appendLine(sb);

        // Items
        sb.append(String.format("%-28s%6s%8s%n", "Item", "Qty", "Price"));
        appendDivider(sb);
        for (OrderItem item : order.getItems()) {
            sb.append(String.format("%-28s%6d%8.2f%n",
                    truncate(item.getProduct().getName(), 28),
                    item.getQuantity(),
                    item.getPrice()));

            // Toppings
            if (!item.getToppings().isEmpty()) {
                String toppingNames = item.getToppings().stream()
                        .map(Topping::getName)
                        .sorted()
                        .collect(Collectors.joining(", "));
                sb.append(String.format("  + %s%n", truncate(toppingNames, LINE_WIDTH - 4)));
            }
        }
        appendDivider(sb);

        // Total
        sb.append(String.format("%-28s%14.2f%n", "TOTAL:", order.getTotal()));
        appendLine(sb);

        // Barcode placeholder (ESC/POS barcode command would go here in real implementation)
        appendCentered(sb, "* " + String.format("%012d", order.getId()) + " *");
        appendLine(sb);

        // Footer
        appendCentered(sb, "Thank you for your order!");
        appendCentered(sb, "Have a great day!");
        sb.append("\n\n\n"); // Feed paper

        return sb.toString();
    }

    private void appendCentered(StringBuilder sb, String text) {
        int padding = Math.max(0, (LINE_WIDTH - text.length()) / 2);
        sb.append(" ".repeat(padding)).append(text).append("\n");
    }

    private void appendLine(StringBuilder sb) {
        sb.append("\n");
    }

    private void appendDivider(StringBuilder sb) {
        sb.append("-".repeat(LINE_WIDTH)).append("\n");
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***" + (at >= 0 ? email.substring(at) : "");
        return email.charAt(0) + "***" + email.substring(at);
    }

    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }
}
