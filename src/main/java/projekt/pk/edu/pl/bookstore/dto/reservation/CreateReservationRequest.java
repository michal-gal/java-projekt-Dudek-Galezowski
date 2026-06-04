package projekt.pk.edu.pl.bookstore.dto.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateReservationRequest(
        @NotNull
        @Positive
        Long bookId
) {
}
