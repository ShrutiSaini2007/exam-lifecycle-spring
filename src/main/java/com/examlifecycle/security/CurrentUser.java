package com.examlifecycle.security;

import com.examlifecycle.domain.Role;

/** The identity carried on the request, decoded straight from the JWT — no DB hit needed per request. */
public record CurrentUser(Long id, String username, String roleName) {
    public Role role() {
        return Role.valueOf(roleName);
    }
}
