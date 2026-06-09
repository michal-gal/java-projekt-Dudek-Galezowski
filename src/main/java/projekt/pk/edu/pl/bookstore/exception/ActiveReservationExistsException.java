package projekt.pk.edu.pl.bookstore.exception;

public class ActiveReservationExistsException extends RuntimeException {
    public ActiveReservationExistsException(Long bookId) {
        super("Użytkownik posiada już aktywną rezerwację dla książki o ID: " + bookId);
    }
}
