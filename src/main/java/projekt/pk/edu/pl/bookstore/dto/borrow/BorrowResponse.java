package projekt.pk.edu.pl.bookstore.dto.borrow;

import java.time.LocalDateTime;

public record BorrowResponse(
        Long id,
        Long userId,
        String userEmail,
        Long bookId,
        String bookTitle,
        Long reservationId,
        String status,
        LocalDateTime borrowedAt,
        LocalDateTime dueAt,
        LocalDateTime returnedAt
) {
}
