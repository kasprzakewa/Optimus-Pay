package com.optimusprice.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
/**
 * Represents a payment variant for an order: a combination of payment methods and their respective amounts.
 */
public class Variant {
    public Map<String, Double> methods; // Method ID -> amount paid
    public double discount; // Total discount for this variant
    public String orderId; // Associated order ID
}
