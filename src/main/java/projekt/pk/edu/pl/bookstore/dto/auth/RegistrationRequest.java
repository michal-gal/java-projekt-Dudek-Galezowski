package projekt.pk.edu.pl.bookstore.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RegistrationRequest", description = "Dane potrzebne do rejestracji nowego czytelnika.")
public record RegistrationRequest(
        @Schema(description = "Imię użytkownika.", example = "Jan")
        @NotBlank
        @Size(max = 100)
        String firstName,
        @Schema(description = "Nazwisko użytkownika.", example = "Kowalski")
        @NotBlank
        @Size(max = 100)
        String lastName,
        @Schema(description = "Adres e-mail użytkownika.", example = "jan.kowalski@example.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email,
        @Schema(description = "Hasło użytkownika.", example = "superhaslo123")
        @NotBlank
        @Size(min = 8, max = 72)
        String password
) {
}
