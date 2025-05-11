package com.optimusprice.exception;

/**
 * Thrown when a required input file is not found.
 */
public class MissingFileException extends RuntimeException {
    public MissingFileException(String message) {
        super(message);
        System.err.println("\u001B[31mERROR: " + message + "\u001B[0m");
    }
}
