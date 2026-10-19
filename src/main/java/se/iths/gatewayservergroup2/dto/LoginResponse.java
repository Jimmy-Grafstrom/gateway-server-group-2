package se.iths.gatewayservergroup2.dto;

import java.util.List;

public record LoginResponse(
        Long expiresIn,
        String subject,
        List<String> roles
) {}