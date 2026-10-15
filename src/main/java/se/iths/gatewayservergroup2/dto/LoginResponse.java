package se.iths.gatewayservergroup2.dto;

import java.util.List;

public record LoginResponse(String accessToken, String subject, List<String> roles) {}