package com.optimuspay.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Variant {
    public Map<String, Double> methods;
    public double discount;
    public String orderId;

    @Override
public String toString() {
    return "Variant{" +
            "methods=" + methods +
            ", discount=" + discount +
            ", orderId='" + orderId + '\'' +
            '}';
}
}


