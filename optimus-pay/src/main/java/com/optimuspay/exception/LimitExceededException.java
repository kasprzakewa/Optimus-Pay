package com.optimuspay.exception;

public class LimitExceededException extends RuntimeException{

    private static final String RED = "\u001B[31m";
    private static final String RESET = "\u001B[0m";
    
    public LimitExceededException(String message) {
        super(message);
        System.err.println(RED + "BŁĄD: " + message + RESET);
    }
}
