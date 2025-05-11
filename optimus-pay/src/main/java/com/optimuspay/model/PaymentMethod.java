package com.optimuspay.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PaymentMethod {
    private String id;
    private double discount;
    private double limit;
    private double remainingLimit;

    public PaymentMethod(String id, double discount, double limit) {
        this.id = id;
        this.discount = discount;
        this.limit = limit;
        this.remainingLimit = limit;
    }
}
