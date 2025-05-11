package com.optimusprice.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import com.optimusprice.exception.NoOptimalSolutionException;

import com.optimusprice.model.Order;
import com.optimusprice.model.PaymentMethod;
import com.optimusprice.model.Variant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * SolverService uses linear optimization to assign orders to payment methods in a way that maximizes total discount and
 * respects payment method limits.
 */
@AllArgsConstructor
@Getter
@Setter
public class SolverService {
    private List<Order> orders;
    private Map<String, PaymentMethod> paymentMethods;

    /**
     * Solves the optimization problem and distributes payments across available methods.
     *
     * @return Map of updated payment methods after solving
     */
    public Map<String, PaymentMethod> solve() {

        if (orders == null || orders.isEmpty()) {
            return paymentMethods;
        }

        List<Variant> optSolution = findOptSolution();
        distributePoints(optSolution);

        return paymentMethods;
    }

    /**
     * Builds and solves the optimization model using Google OR-Tools.
     *
     * @return List of selected payment variants for orders
     */
    private List<Variant> findOptSolution() {
        Loader.loadNativeLibraries();
        MPSolver solver = MPSolver.createSolver("SCIP");

        List<List<Variant>> allVariants = new ArrayList<>();
        for (Order order : orders) {
            allVariants.add(generateVariants(order));
        }

        Map<String, MPVariable> vars = new HashMap<>();
        Map<String, Variant> varToVariant = new HashMap<>();

        for (int i = 0; i < allVariants.size(); i++) {
            List<Variant> variants = allVariants.get(i);
            for (int j = 0; j < variants.size(); j++) {
                String varName = "x_" + i + "_" + j;
                MPVariable var = solver.makeBoolVar(varName);
                vars.put(varName, var);
                varToVariant.put(varName, variants.get(j));
            }
        }

        for (int i = 0; i < allVariants.size(); i++) {
            MPConstraint constraint = solver.makeConstraint(1, 1);
            for (int j = 0; j < allVariants.get(i).size(); j++) {
                constraint.setCoefficient(vars.get("x_" + i + "_" + j), 1);
            }
        }

        for (String method : paymentMethods.keySet()) {
            double limit = paymentMethods.get(method).getLimit();
            MPConstraint constraint = solver.makeConstraint(0, limit);
            for (int i = 0; i < allVariants.size(); i++) {
                for (int j = 0; j < allVariants.get(i).size(); j++) {
                    Variant v = allVariants.get(i).get(j);
                    double amount = v.methods.getOrDefault(method, 0.0);
                    constraint.setCoefficient(vars.get("x_" + i + "_" + j), amount);
                }
            }
        }

        MPObjective objective = solver.objective();
        for (String varName : vars.keySet()) {
            double discount = varToVariant.get(varName).discount;
            objective.setCoefficient(vars.get(varName), discount);
        }
        objective.setMaximization();

        MPSolver.ResultStatus resultStatus = solver.solve();
        if (resultStatus != MPSolver.ResultStatus.OPTIMAL) {
            throw new NoOptimalSolutionException("Optimal solution not found.");
        }

        List<Variant> selected = new ArrayList<>();
        for (String varName : vars.keySet()) {
            if (vars.get(varName).solutionValue() == 1.0) {
                Variant v = varToVariant.get(varName);
                selected.add(v);

                for (Map.Entry<String, Double> entry : v.methods.entrySet()) {
                    String method = entry.getKey();
                    paymentMethods.get(method)
                            .setRemainingLimit(paymentMethods.get(method).getRemainingLimit() - entry.getValue());
                }
            }
        }
        return selected;
    }

    /**
     * Generates all valid payment variants for a given order based on its value, promotions, and available payment
     * methods.
     *
     * @param order
     *            The order for which to generate variants
     *
     * @return List of valid variants
     */
    private List<Variant> generateVariants(Order order) {
        if (order.getValue() <= 0) {
            return List.of();
        }

        List<Variant> variants = new ArrayList<>();
        double value = order.getValue();
        List<String> promos = order.getPromotions() != null ? order.getPromotions() : new ArrayList<>();

        // All with promo card
        for (String method : promos) {
            PaymentMethod pm = paymentMethods.get(method);
            if (pm != null && pm.getLimit() >= value) {
                double discount = pm.getDiscount();
                double pay = value * (1 - discount / 100.0);
                variants.add(new Variant(Map.of(method, pay), value - pay, order.getId()));
            }
        }

        if (paymentMethods.containsKey("PUNKTY")) {
            PaymentMethod punkty = paymentMethods.get("PUNKTY");

            // All with points
            if (punkty.getLimit() >= value) {
                double discount = punkty.getDiscount();
                double pay = value * (1 - discount / 100.0);
                variants.add(new Variant(Map.of("PUNKTY", pay), value - pay, order.getId()));
            }

            // Part with points + one with traditional method
            double minPoints = 0.1 * value;
            if (paymentMethods.get("PUNKTY").getLimit() >= minPoints) {
                double cashPart = value - minPoints - 0.10 * value;
                for (Map.Entry<String, PaymentMethod> entry : paymentMethods.entrySet()) {
                    String method = entry.getKey();
                    if (method.equals("PUNKTY"))
                        continue;
                    if (entry.getValue().getLimit() >= cashPart) {
                        Map<String, Double> map = new HashMap<>();
                        map.put("PUNKTY", minPoints);
                        map.put(method, cashPart);
                        variants.add(new Variant(map, 0.10 * value, order.getId()));
                    }
                }
            }
        }

        // All without promo
        for (String method : paymentMethods.keySet()) {
            if (method.equals("PUNKTY"))
                continue;
            if (promos != null) {
                if (promos.contains(method))
                    continue;
            }

            if (paymentMethods.get(method).getLimit() >= value) {
                variants.add(new Variant(Map.of(method, value), 0.0, order.getId()));
            }
        }

        return variants;
    }

    /**
     * Distributes remaining loyalty points across selected variants in an optimized way.
     *
     * @param variants
     *            List of selected variants
     */
    private void distributePoints(List<Variant> variants) {
        List<Variant> pointedVariants = new ArrayList<>();
        for (Variant variant : variants) {
            if (variant.methods.containsKey("PUNKTY")) {
                pointedVariants.add(variant);
            }
        }

        double remainingPoints = paymentMethods.get("PUNKTY").getRemainingLimit();
        for (Variant variant : pointedVariants) {
            List<String> methods = new ArrayList<>(variant.methods.keySet());
            methods.remove("PUNKTY");
            if (methods.isEmpty()) {
                continue;
            } else {
                String method = methods.get(0);
                double amount = variant.methods.get(method);
                if (remainingPoints < amount) {
                    variant.methods.put(method, amount - remainingPoints);
                    variant.methods.put("PUNKTY", variant.methods.get("PUNKTY") + remainingPoints);
                    paymentMethods.get(method)
                            .setRemainingLimit(paymentMethods.get(method).getRemainingLimit() + remainingPoints);
                    paymentMethods.get("PUNKTY").setRemainingLimit(0);
                    break;
                }
            }
        }
    }
}
