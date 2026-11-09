package com.vnsnord.liqflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the LiqFlow application.
 *
 * <p>Bootstraps the Spring Boot context, including the JPA repositories,
 * Flyway database migrations, and all REST controllers.</p>
 */
@SpringBootApplication
public class LiqflowApplication
{

    /**
     * Starts the LiqFlow application.
     *
     * @param args command-line arguments passed to the Spring Boot runner
     */
    public static void main(String[] args)
    {
        SpringApplication.run(LiqflowApplication.class, args);
    }
}