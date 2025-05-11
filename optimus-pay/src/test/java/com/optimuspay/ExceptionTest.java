package com.optimuspay;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.optimuspay.exception.LimitExceededException;
import com.optimuspay.exception.NoOptimalSolutionException;
import com.optimuspay.model.Order;
import com.optimuspay.model.PaymentMethod;
import com.optimuspay.service.SolverService;

public class ExceptionTest {

    private Map<String, PaymentMethod> paymentMethods;

    @BeforeEach
    public void setUp() {
        paymentMethods = new HashMap<>();
        paymentMethods.put("KARTA1", new PaymentMethod("KARTA1", 5.0, 100.0));
        paymentMethods.put("KARTA2", new PaymentMethod("KARTA2", 15.0, 100.0));
        paymentMethods.put("PUNKTY", new PaymentMethod("PUNKTY", 10.0, 50.0));
    }

    @Test
    public void testOrdersThatExceedPaymentLimit() {
        List<Order> orders = List.of(new Order("ORDER6", 300.0, List.of()));
        SolverService solverService = new SolverService(orders, paymentMethods);

        assertThrows(LimitExceededException.class, () -> {
            solverService.solve();
        });
    }

    @Test
    public void testOrderWithNoAvailablePaymentMethods() throws NoSuchMethodException, SecurityException {
        List<Order> orders = List.of(new Order("ORDER5", 250.0, List.of()));
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
