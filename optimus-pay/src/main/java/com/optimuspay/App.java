package com.optimuspay;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.optimuspay.model.Order;
import com.optimuspay.model.PaymentMethod;
import com.optimuspay.service.SolverService;

public class App {
    public static void main(String[] args){

        if (args.length != 2) {
            System.out.println("Usage: java -jar demo.jar <orders.json> <paymentmethods.json>");
            return;
        }

        String ordersPath = args[0];
        String paymentMethodsPath = args[1];

        ObjectMapper objectMapper = new ObjectMapper();
        List<Order> orders = List.of();
        Map<String, PaymentMethod> paymentMethods = new HashMap<>();

        try {
            orders = objectMapper.readValue(new File(ordersPath), objectMapper.getTypeFactory().constructCollectionType(List.class, Order.class));
            
            List<PaymentMethod> paymentMethodsList = objectMapper.readValue(new File(paymentMethodsPath), objectMapper.getTypeFactory().constructCollectionType(List.class, PaymentMethod.class));
            for (PaymentMethod paymentMethod : paymentMethodsList) {
                paymentMethods.put(paymentMethod.getId(), paymentMethod);
                paymentMethod.setRemainingLimit(paymentMethod.getLimit());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        SolverService service = new SolverService(orders, paymentMethods);
        Map<String, PaymentMethod> distributedPayments = service.solve();

        for (Map.Entry<String, PaymentMethod> entry : distributedPayments.entrySet()) {
            String method = entry.getKey();
            System.out.println(method + " " + String.format("%.2f", entry.getValue().getLimit() - entry.getValue().getRemainingLimit()));
        }
    }
}
