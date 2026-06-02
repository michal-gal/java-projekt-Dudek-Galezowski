package projekt.pk.edu.pl.bookstore.dto.auth;

public record CurrentUserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role,
        boolean isActive
) {
}
