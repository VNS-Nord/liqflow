package com.vnsnord.liqflow.identity.user;

import com.vnsnord.liqflow.identity.access.Permission;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Loads {@link UserDetails} from the application's {@link UserRepository} for
 * Spring Security authentication.
 *
 * <p>Usernames are normalized to trimmed lower case, and the user's effective
 * authorities are derived from the distinct permission codes of all assigned
 * roles.</p>
 */
@Service
public class CustomUserDetailsService implements UserDetailsService
{
	private final UserRepository userRepository;

	/**
	 * Creates a new user-details service backed by the given repository.
	 *
	 * @param userRepository the user repository
	 */
	public CustomUserDetailsService(UserRepository userRepository)
	{
		this.userRepository = userRepository;
	}

	/**
	 * Loads the user with the given username, including its granted authorities.
	 *
	 * @param username the username to look up
	 * @return the matching user details
	 * @throws UsernameNotFoundException if no user exists with the given username
	 */
	@SuppressWarnings("NullableProblems")
	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String username)
	{
		String normalizedUsername = username.trim()
		                                    .toLowerCase(Locale.ROOT);
		User user = userRepository.findByUsername(normalizedUsername)
		                          .orElseThrow(() -> new UsernameNotFoundException("User not found"));
		List<SimpleGrantedAuthority> authorities = user.getRoles()
		                                               .stream()
													   .flatMap(role -> role.getPermissions()
		                                                                    .stream())
													   .map(Permission::getCode)
													   .distinct()
													   .map(SimpleGrantedAuthority::new)
													   .toList();

		return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
																 .password(user.getPasswordHash())
																 .disabled(! user.isEnabled())
																 .authorities(authorities)
																 .build();
	}
}
