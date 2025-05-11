package com.optimusprice;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.optimusprice.model.Order;
import com.optimusprice.model.PaymentMethod;
import com.optimusprice.service.SolverService;
import com.optimusprice.exception.MissingFileException;

/**
 * Main application class for processing orders and distributing payment across available payment methods to maximize
 * discount.
 */
public class App {
    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Usage: java -jar optimus-price.jar <orders.json> <paymentmethods.json>");
            return;
        }

        // Validate file paths and read input
        String ordersPath = args[0];
        String paymentMethodsPath = args[1];

        File ordersFile = new File(ordersPath);
        File paymentMethodsFile = new File(paymentMethodsPath);

        if (!ordersFile.exists()) {
            throw new MissingFileException("Orders file not found: " + ordersPath);
        }

        if (!paymentMethodsFile.exists()) {
            throw new MissingFileException("Payment methods file not found: " + paymentMethodsPath);
        }

        // Parse orders and payment methods
        ObjectMapper objectMapper = new ObjectMapper();
        List<Order> orders = List.of();
        Map<String, PaymentMethod> paymentMethods = new HashMap<>();

        try {
            orders = objectMapper.readValue(ordersFile,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Order.class));

            List<PaymentMethod> paymentMethodsList = objectMapper.readValue(paymentMethodsFile,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PaymentMethod.class));
            for (PaymentMethod paymentMethod : paymentMethodsList) {
                paymentMethods.put(paymentMethod.getId(), paymentMethod);
                paymentMethod.setRemainingLimit(paymentMethod.getLimit());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Solve and print results
        SolverService service = new SolverService(orders, paymentMethods);
        Map<String, PaymentMethod> distributedPayments = service.solve();

        for (Map.Entry<String, PaymentMethod> entry : distributedPayments.entrySet()) {
            String method = entry.getKey();
            System.out.println(method + " "
                    + String.format("%.2f", entry.getValue().getLimit() - entry.getValue().getRemainingLimit()));
        }
    }
}
