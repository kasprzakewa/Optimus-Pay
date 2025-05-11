package com.optimusprice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
/**
 * Represents a payment method, including available limit and discount.
 */
public class PaymentMethod {
    private String id; // Identifier (e.g., "PUNKTY", "mZysk")
    private double discount; // Discount percentage
    private double limit; // Initial available amount
    private double remainingLimit; // Remaining amount after spending

    /**
     * Constructs a PaymentMethod and initializes remaining limit to total limit.
     *
     * @param id
     *            Identifier
     * @param discount
     *            Discount percentage
     * @param limit
     *            Spending limit
     */
    public PaymentMethod(String id, double discount, double limit) {
        this.id = id;
        this.discount = discount;
        this.limit = limit;
        this.remainingLimit = limit;
    }
}
