package com.specimen.sms.web.dto;

public record LoginResponse(
        String token,
        String username,
        String role) {
}
