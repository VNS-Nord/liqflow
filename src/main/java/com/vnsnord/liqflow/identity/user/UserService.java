package com.vnsnord.liqflow.identity.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for managing users.
 */
@Service
public class UserService
{
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final UserMapper userMapper;

	/**
	 * Creates a new user service with the given dependencies.
	 *
	 * @param userRepository  the user repository
	 * @param passwordEncoder the encoder used to hash passwords
	 * @param userMapper      the user mapper
	 */
	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper)
	{
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.userMapper = userMapper;
	}

	/**
	 * Creates a new user after hashing the password and checking that the
	 * username and email are not already taken.
	 *
	 * @param request the user creation request
	 * @return the response DTO of the saved user
	 * @throws IllegalArgumentException if a user with the same email or username already exists
	 */
	@Transactional
	public UserResponse createUser(CreateUserRequest request)
	{
		String passwordHash = passwordEncoder.encode(request.password());
		User user = userMapper.toEntity(request, passwordHash);
		if (userRepository.existsByEmail(user.getEmail()))
		{
			throw new IllegalArgumentException("User with email " + user.getEmail() + " already exists");
		}
		if (userRepository.existsByUsername(user.getUsername()))
		{
			throw new IllegalArgumentException("User with username " + user.getUsername() + " already exists");
		}
		return userMapper.toResponse(userRepository.save(user));
	}
}
