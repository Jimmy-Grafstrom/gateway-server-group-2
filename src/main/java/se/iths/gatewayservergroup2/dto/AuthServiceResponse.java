package se.iths.gatewayservergroup2.dto;

import java.util.List;

public record AuthServiceResponse(
        String accessToken,
        Long expiresIn,
        String subject,
        List<String> roles
) {
}
