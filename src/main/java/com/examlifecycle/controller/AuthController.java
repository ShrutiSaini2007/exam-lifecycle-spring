package com.examlifecycle.controller;

import com.examlifecycle.domain.Role;
import com.examlifecycle.dto.AuthDtos.*;
import com.examlifecycle.security.AuthUtil;
import com.examlifecycle.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** Public. The only unauthenticated route in the whole API. */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return userService.login(req);
    }

    /** Admin-only — this is what makes "no public sign-up" true. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView register(@Valid @RequestBody RegisterRequest req) {
        var admin = AuthUtil.current();
        AuthUtil.requireRole(admin, Role.ADMIN);
        return userService.register(admin, req);
    }

    @GetMapping("/me")
    public UserView me() {
        var user = AuthUtil.current();
        return userService.getById(user.id());
    }

    @GetMapping("/users")
    public List<UserView> listUsers() {
        AuthUtil.requireRole(AuthUtil.current(), Role.ADMIN);
        return userService.listUsers();
    }

    /** Admin-only — removes a registered user by ID. */
   /** Admin-only — removes a registered user by ID with self-deletion protection. */
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        var admin = AuthUtil.current();
        AuthUtil.requireRole(admin, Role.ADMIN);

        // Prevent self-deletion
        if (admin.id().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own active admin account.");
        }

        userService.deleteUser(id);
    }
}
