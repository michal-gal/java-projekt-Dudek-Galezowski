package projekt.pk.edu.pl.bookstore.dto.book;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(name = "BookRequest", description = "Dane książki do utworzenia lub aktualizacji.")
public class BookRequest {

    @Schema(description = "Numer ISBN książki.", example = "9780132350884")
    @NotBlank(message = "ISBN nie może być pusty")
    @Size(max = 20, message = "ISBN może mieć maksymalnie 20 znaków")
    private String isbn;

    @Schema(description = "Tytuł książki.", example = "Clean Code")
    @NotBlank(message = "Tytuł nie może być pusty")
    @Size(max = 255, message = "Tytuł może mieć maksymalnie 255 znaków")
    private String title;

    @Schema(description = "Autor książki.", example = "Robert C. Martin")
    @NotBlank(message = "Autor nie może być pusty")
    @Size(max = 255, message = "Autor może mieć maksymalnie 255 znaków")
    private String author;

    @Schema(description = "Wydawnictwo.", example = "Prentice Hall")
    @Size(max = 255, message = "Nazwa wydawnictwa jest za długa")
    private String publisher;

    @Schema(description = "Rok publikacji.", example = "2008")
    @Min(value = 1000, message = "Niepoprawny rok publikacji")
    private Integer publicationYear;

    @Schema(description = "Łączna liczba egzemplarzy.", example = "5")
    @NotNull(message = "Liczba egzemplarzy jest wymagana")
    @Min(value = 0, message = "Liczba egzemplarzy nie może być ujemna")
    private Integer totalCopies;
}