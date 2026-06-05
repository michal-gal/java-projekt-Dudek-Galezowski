package projekt.pk.edu.pl.bookstore.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(name = "ApiErrorResponse", description = "Spójny format błędu zwracany przez API.")
public record ApiErrorResponse(
        @Schema(description = "Czytelny komunikat błędu.", example = "Brak dostępnych egzemplarzy książki o ID: 1")
        String message,
        @Schema(description = "Błędy walidacji pól, jeśli wystąpiły.", example = "{\"email\":\"must be a well-formed email address\"}")
        Map<String, String> fieldErrors
) {
}
