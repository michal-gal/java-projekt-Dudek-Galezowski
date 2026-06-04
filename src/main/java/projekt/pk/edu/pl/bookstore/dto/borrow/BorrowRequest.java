package projekt.pk.edu.pl.bookstore.dto.borrow;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "BorrowRequest", description = "Dane potrzebne do zrealizowania wypożyczenia z aktywnej rezerwacji.")
public record BorrowRequest(
        @Schema(description = "Identyfikator aktywnej rezerwacji.", example = "10")
        @NotNull
        @Positive
        Long reservationId
) {
}
