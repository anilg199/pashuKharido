package com.pashumandi.exception;

public class MaxImageLimitExceededException extends RuntimeException {

    public MaxImageLimitExceededException(String message) {
        super(message);
    }
}
