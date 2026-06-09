package projekt.pk.edu.pl.bookstore.dto.auth;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
