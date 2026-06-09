package projekt.pk.edu.pl.bookstore.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginRequest", description = "Dane logowania użytkownika.")
public record LoginRequest(
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
