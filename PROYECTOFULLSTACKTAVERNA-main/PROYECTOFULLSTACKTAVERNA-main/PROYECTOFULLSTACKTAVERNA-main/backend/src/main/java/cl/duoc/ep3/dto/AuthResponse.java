package cl.duoc.ep3.dto;

import java.time.Instant;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, AccountResponse user) {
}
