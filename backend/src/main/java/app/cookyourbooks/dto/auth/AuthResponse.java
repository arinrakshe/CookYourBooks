package app.cookyourbooks.dto.auth;

import lombok.Builder;

@Builder
public record AuthResponse(
        String token,
        String tokenType,
        UserSummary user) {

    @Builder
    public record UserSummary(Long id, String email, String displayName) {
    }
}
