package com.nahuel.ecommerce.exceptions;

import java.time.Instant;

public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}
