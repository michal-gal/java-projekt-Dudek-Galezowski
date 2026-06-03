package projekt.pk.edu.pl.bookstore.dto.auth;

import java.util.Map;

public record ApiErrorResponse(
        String message,
        Map<String, String> fieldErrors
) {
}
