package projekt.pk.edu.pl.bookstore.dto.auth;

public record RegistrationResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String role
) {
}
