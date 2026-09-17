package com.examlifecycle.security;

import com.examlifecycle.domain.Role;
import com.examlifecycle.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;

public final class AuthUtil {

    private AuthUtil() {}

    public static CurrentUser current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CurrentUser cu)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "missing or invalid bearer token");
        }
        return cu;
    }

    /** Allowed if the user's role is in {@code allowed}, OR the user is ADMIN — same rule as the Python prototype. */
    public static void requireRole(CurrentUser user, Role... allowed) {
        if (user.role() == Role.ADMIN) return;
        if (Arrays.asList(allowed).contains(user.role())) return;
        throw new ApiException(HttpStatus.FORBIDDEN,
                "role '" + user.role() + "' not permitted; requires one of " + Arrays.toString(allowed));
    }
}
