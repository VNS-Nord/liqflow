package com.vnsnord.liqflow.identity.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for managing users under {@code /api/v1/users}.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController
{
    private final UserService userService;

    /**
     * Creates a new user controller backed by the given service.
     *
     * @param userService the user application service
     */
    public UserController(UserService userService)
    {
        this.userService = userService;
    }

    /**
     * Creates a new user.
     *
     * @param request the user creation request
     * @return the created user with HTTP 201
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_MANAGE')")
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request)
    {
        return userService.createUser(request);
    }
}
