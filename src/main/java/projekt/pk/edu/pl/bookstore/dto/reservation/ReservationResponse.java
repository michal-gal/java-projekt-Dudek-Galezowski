package projekt.pk.edu.pl.bookstore.dto.reservation;

import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        Long userId,
        Long bookId,
        String bookTitle,
        String status,
        LocalDateTime reservedAt,
        LocalDateTime expiresAt
) {
}
