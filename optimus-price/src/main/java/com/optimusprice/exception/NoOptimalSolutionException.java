package com.optimusprice.exception;

/**
 * Thrown when the solver fails to find an optimal solution.
 */
public class NoOptimalSolutionException extends RuntimeException {
    public NoOptimalSolutionException(String message) {
        super(message);
        System.err.println("\u001B[31mERROR: " + message + "\u001B[0m");
    }
}
