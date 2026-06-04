package projekt.pk.edu.pl.bookstore.dto.borrow;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BorrowRequest(
        @NotNull
        @Positive
        Long reservationId
) {
}
