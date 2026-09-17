package com.examlifecycle.service;

import com.examlifecycle.domain.User;
import com.examlifecycle.dto.AuthDtos.*;
import com.examlifecycle.exception.ApiException;
import com.examlifecycle.repository.UserRepository;
import com.examlifecycle.security.CurrentUser;
import com.examlifecycle.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtService jwtService, AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public UserView register(CurrentUser admin, RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ApiException(HttpStatus.CONFLICT, "username already taken");
        }
        User user = new User(req.username(), passwordEncoder.encode(req.password()), req.role());
        user = userRepository.save(user);
        auditService.record(admin.id(), "CREATE_USER", "user", user.getId(),
                java.util.Map.of("role", req.role().name(), "username", req.username()));
        return new UserView(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }

    @Transactional
    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username()).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            auditService.record(null, "LOGIN_FAILED", "user", null, java.util.Map.of("username", req.username()));
            throw new ApiException(HttpStatus.UNAUTHORIZED, "invalid credentials");
        }
        String token = jwtService.issueToken(user);
        auditService.record(user.getId(), "LOGIN", "user", user.getId());
        return new TokenResponse(token, new UserView(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt()));
    }

    public List<UserView> listUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserView(u.getId(), u.getUsername(), u.getRole(), u.getCreatedAt()))
                .toList();
    }

    public UserView getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "user not found"));
        return new UserView(user.getId(), user.getUsername(), user.getRole(), user.getCreatedAt());
    }

    public void deleteUser(Long id) {
    User user = userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    
    userRepository.delete(user);
}
}
