package com.optimusprice.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
/**
 * Represents a customer's order.
 */
public class Order {
    private String id; // Order ID
    private double value; // Total order value
    private List<String> promotions; // List of applicable promotional methods
}
