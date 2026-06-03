package projekt.pk.edu.pl.bookstore.dto.book;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookRequest {

    @NotBlank(message = "ISBN nie może być pusty")
    @Size(max = 20, message = "ISBN może mieć maksymalnie 20 znaków")
    private String isbn;

    @NotBlank(message = "Tytuł nie może być pusty")
    @Size(max = 255, message = "Tytuł może mieć maksymalnie 255 znaków")
    private String title;

    @NotBlank(message = "Autor nie może być pusty")
    @Size(max = 255, message = "Autor może mieć maksymalnie 255 znaków")
    private String author;

    @Size(max = 255, message = "Nazwa wydawnictwa jest za długa")
    private String publisher;

    @Min(value = 1000, message = "Niepoprawny rok publikacji")
    private Integer publicationYear;

    @NotNull(message = "Liczba egzemplarzy jest wymagana")
    @Min(value = 0, message = "Liczba egzemplarzy nie może być ujemna")
    private Integer totalCopies;
}