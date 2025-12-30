package com.example.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Mock External Authentication Server
 * 
 * This is a simple mock server that simulates an external authentication system
 * for testing OAuth2 integration.
 */
@SpringBootApplication
public class MockExternalServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MockExternalServerApplication.class, args);
    }
}
