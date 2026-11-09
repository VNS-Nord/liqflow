package com.vnsnord.liqflow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * Configures the password encoder used to hash and verify user passwords.
 *
 * <p>Passwords are stored using Argon2, wrapped in a delegating encoder so the
 * stored hash carries an {@code {argon2}} prefix and algorithms can be migrated
 * later without breaking existing credentials.</p>
 */
@Configuration
public class PasswordConfig
{
	/**
	 * Creates the application's password encoder.
	 *
	 * @return a delegating password encoder backed by Argon2
	 */
	@Bean
	public PasswordEncoder passwordEncoder()
	{
		PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
		return new DelegatingPasswordEncoder("argon2", Map.of("argon2", argon2));
	}
}
