package com.specimen.sms.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Helper to read the authenticated user from the security context. */
public final class CurrentUser {

    private CurrentUser() {
    }

    private static Authentication auth() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken) ? a : null;
    }

    public static boolean isAuthenticated() {
        return auth() != null;
    }

    public static String username() {
        Authentication a = auth();
        return a == null ? "system" : a.getName();
    }

    public static boolean isAdmin() {
        Authentication a = auth();
        return a != null && a.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_LAB_ADMIN"));
    }
}
