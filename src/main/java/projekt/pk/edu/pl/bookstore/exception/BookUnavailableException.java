package projekt.pk.edu.pl.bookstore.exception;

public class BookUnavailableException extends RuntimeException {
    public BookUnavailableException(Long bookId) {
        super("Brak dostępnych egzemplarzy książki o ID: " + bookId);
    }
}
