package projekt.pk.edu.pl.bookstore.dto.reservation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "CreateReservationRequest", description = "Dane potrzebne do utworzenia rezerwacji książki.")
public record CreateReservationRequest(
        @Schema(description = "Identyfikator książki do rezerwacji.", example = "1")
        @NotNull
        @Positive
        Long bookId
) {
}
