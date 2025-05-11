package com.optimusprice;

import com.optimusprice.model.Order;
import com.optimusprice.model.PaymentMethod;
import com.optimusprice.model.Variant;
import com.optimusprice.service.SolverService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the SolverService logic, including variant generation, payment constraints, and distribution of
 * loyalty points.
 */
public class SolverServiceTest {

    private Map<String, PaymentMethod> paymentMethods;
    private List<Order> orders;

    /**
     * Prepares a set of mock payment methods and orders for test use.
     */
    @BeforeEach
    public void setUp() {
        paymentMethods = new HashMap<>();
        paymentMethods.put("KARTA1", new PaymentMethod("KARTA1", 5.0, 100.0));
        paymentMethods.put("KARTA2", new PaymentMethod("KARTA2", 15.0, 100.0));
        paymentMethods.put("PUNKTY", new PaymentMethod("PUNKTY", 10.0, 50.0));

        orders = List.of(new Order("ORDER1", 30.0, List.of("KARTA1")), new Order("ORDER2", 40.0, List.of("KARTA2")),
                new Order("ORDER3", 10.0, List.of("KARTA1", "KARTA2")), new Order("ORDER4", 50.0, List.of()));
    }

    /**
     * Verifies that with no orders, all payment method limits remain unchanged.
     */
    @Test
    public void testNoOrders() {
        SolverService solverService = new SolverService(List.of(), paymentMethods);
        Map<String, PaymentMethod> result = solverService.solve();

        for (PaymentMethod method : result.values()) {
            assertThat(method.getRemainingLimit()).isEqualTo(method.getLimit());
        }
    }

    /**
     * Verifies behavior when a payment method has zero spending limit.
     */
    @Test
    public void testZeroLimitPaymentMethod() {
        paymentMethods.put("ZERO", new PaymentMethod("ZERO", 10.0, 0.0));
        List<Order> localOrders = List.of(new Order("ORDER6", 10.0, List.of("ZERO")));

        SolverService solverService = new SolverService(localOrders, paymentMethods);
        Map<String, PaymentMethod> result = solverService.solve();

        assertThat(result.get("ZERO").getRemainingLimit()).isEqualTo(0.0);
    }

    /**
     * Ensures no variants are generated for an order with zero value.
     */
    @Test
    public void testGenerateVariantsForZeroValuedOrder() throws Exception {
        Order order = new Order("ORDER5", 0.0, List.of("KARTA1"));
        SolverService solverService = new SolverService(List.of(order), paymentMethods);

        Method method = SolverService.class.getDeclaredMethod("generateVariants", Order.class);
        method.setAccessible(true);

        List<?> result = (List<?>) method.invoke(solverService, order);
        assertThat(result).isEmpty();
    }

    /**
     * Tests whether variants are generated for all orders correctly.
     */
    @Test
    public void testGenerateVariants() throws Exception {
        SolverService solverService = new SolverService(orders, paymentMethods);

        Method method = SolverService.class.getDeclaredMethod("generateVariants", Order.class);
        method.setAccessible(true);

        for (Order order : orders) {
            List<?> result = (List<?>) method.invoke(solverService, order);
            assertThat(result).hasSize(paymentMethods.size() + paymentMethods.size() - 1);
        }
    }

    /**
     * Confirms that each order is assigned exactly one payment variant.
     */
    @Test
    public void testEachOrderHasExactlyOnePayment() throws Exception {
        SolverService solverService = new SolverService(orders, paymentMethods);

        Method method = SolverService.class.getDeclaredMethod("findOptSolution");
        method.setAccessible(true);

        List<Variant> result = (List<Variant>) method.invoke(solverService);

        for (Order order : orders) {
            List<Variant> variants = result.stream().filter(variant -> variant.getOrderId().equals(order.getId()))
                    .toList();
            assertThat(variants).hasSize(1);
        }
    }

    /**
     * Ensures that no payment method is used beyond its allowed limit.
     */
    @Test
    public void testPaymentsDoNotExceedMethodLimits() throws Exception {
        SolverService solverService = new SolverService(orders, paymentMethods);

        Method method = SolverService.class.getDeclaredMethod("findOptSolution");
        method.setAccessible(true);

        List<Variant> result = (List<Variant>) method.invoke(solverService);

        for (Variant variant : result) {
            for (Map.Entry<String, Double> entry : variant.getMethods().entrySet()) {
                String methodName = entry.getKey();
                double payment = entry.getValue();
                PaymentMethod paymentMethod = paymentMethods.get(methodName);
                if (paymentMethod != null) {
                    assertThat(payment).isLessThanOrEqualTo(paymentMethod.getLimit());
                }
            }
        }
    }

    /**
     * Confirms that each variant uses no more than two payment methods.
     */
    @Test
    public void testEachVariantHasAtMostTwoPaymentMethods() throws Exception {
        SolverService solverService = new SolverService(orders, paymentMethods);

        Method method = SolverService.class.getDeclaredMethod("findOptSolution");
        method.setAccessible(true);

        List<Variant> result = (List<Variant>) method.invoke(solverService);

        for (Variant variant : result) {
            assertThat(variant.getMethods().size()).isLessThanOrEqualTo(2);
        }
    }

    /**
     * Verifies proper redistribution of loyalty points when they're limited.
     */
    @Test
    public void testDistributePoints() throws Exception {
        Variant variant = new Variant(new HashMap<>(Map.of("PUNKTY", 30.0, "KARTA", 70.0)), 0.0, "order1");

        PaymentMethod punkty = new PaymentMethod("PUNKTY", 100.0, 0.0);
        punkty.setRemainingLimit(20.0);

        PaymentMethod card = new PaymentMethod("KARTA", 200.0, 0.0);
        card.setRemainingLimit(130.0);

        List<Variant> variants = new ArrayList<>();
        variants.add(variant);

        Map<String, PaymentMethod> paymentMethods = new HashMap<>();
        paymentMethods.put("PUNKTY", punkty);
        paymentMethods.put("KARTA", card);

        SolverService solverService = new SolverService(orders, paymentMethods);

        Method method = SolverService.class.getDeclaredMethod("distributePoints", List.class);
        method.setAccessible(true);

        method.invoke(solverService, variants);

        assertThat(variant.getMethods().get("PUNKTY")).isEqualTo(50.0);
        assertThat(variant.getMethods().get("KARTA")).isEqualTo(50.0);

        assertThat(paymentMethods.get("PUNKTY").getRemainingLimit()).isEqualTo(0.0);
        assertThat(paymentMethods.get("KARTA").getRemainingLimit()).isEqualTo(150.0);
    }

    /**
     * Ensures that no variants are created if payment methods can't cover the order.
     */
    @Test
    public void testOrderWithNoAvailablePaymentMethods() throws Exception {
        List<Order> orders = List.of(new Order("ORDER5", 250.0, List.of()));
        SolverService solverService = new SolverService(orders, paymentMethods);

        Method method = SolverService.class.getDeclaredMethod("generateVariants", Order.class);
        method.setAccessible(true);

        List<?> result = (List<?>) method.invoke(solverService, orders.get(0));
        assertThat(result).isEmpty();
    }

    /**
     * Validates full execution of the solve method and its results.
     */
    @Test
    public void testSolve() {
        SolverService solverService = new SolverService(orders, paymentMethods);
        Map<String, PaymentMethod> result = solverService.solve();

        assertThat(result).isNotNull();
        assertThat(result.size()).isEqualTo(paymentMethods.size());

        for (Map.Entry<String, PaymentMethod> entry : result.entrySet()) {
            String method = entry.getKey();
            double totalPaid = entry.getValue().getLimit() - entry.getValue().getRemainingLimit();
            assertThat(totalPaid).isLessThanOrEqualTo(paymentMethods.get(method).getLimit());
        }

        assertThat(result.get("KARTA1").getLimit() - result.get("KARTA1").getRemainingLimit()).isEqualTo(0.00);
        assertThat(result.get("KARTA2").getLimit() - result.get("KARTA2").getRemainingLimit()).isEqualTo(64.50);
        assertThat(result.get("PUNKTY").getLimit() - result.get("PUNKTY").getRemainingLimit()).isEqualTo(50.00);
    }
}
