package com.optimusprice;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.optimusprice.exception.NoOptimalSolutionException;
import com.optimusprice.model.Order;
import com.optimusprice.model.PaymentMethod;
import com.optimusprice.service.SolverService;

/**
 * Unit tests for verifying exception handling in the SolverService, when no valid payment method configuration is
 * possible.
 */
public class ExceptionTest {

    /**
     * Verifies that an exception is thrown when no valid payment method configuration can cover the order amount.
     */
    @Test
    public void testOrderWithNoAvailablePaymentMethods() throws NoSuchMethodException, SecurityException {
        List<Order> orders = List.of(new Order("ORDER5", 250.0, List.of()));
        Map<String, PaymentMethod> paymentMethods = new HashMap<>();
        paymentMethods.put("KARTA1", new PaymentMethod("KARTA1", 5.0, 100.0));
        paymentMethods.put("KARTA2", new PaymentMethod("KARTA2", 15.0, 100.0));
        paymentMethods.put("PUNKTY", new PaymentMethod("PUNKTY", 10.0, 50.0));
        SolverService solverService = new SolverService(orders, paymentMethods);

        Method method = SolverService.class.getDeclaredMethod("findOptSolution");
        method.setAccessible(true);

        try {
            method.invoke(solverService);
            fail("Expected exception to be thrown");
        } catch (Exception e) {
            assertTrue(e.getCause() instanceof NoOptimalSolutionException);
        }
    }
}
