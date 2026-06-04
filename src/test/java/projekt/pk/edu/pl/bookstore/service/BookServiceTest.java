package projekt.pk.edu.pl.bookstore.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import projekt.pk.edu.pl.bookstore.dto.book.BookRequest;
import projekt.pk.edu.pl.bookstore.dto.book.BookResponse;
import projekt.pk.edu.pl.bookstore.entity.Book;
import projekt.pk.edu.pl.bookstore.exception.BusinessConflictException;
import projekt.pk.edu.pl.bookstore.exception.ResourceNotFoundException;
import projekt.pk.edu.pl.bookstore.repository.BookRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void shouldReturnMappedBooksPage() {
        Book book = book(1L, "9780132350884", "Clean Code", 5, 5);
        PageRequest pageable = PageRequest.of(0, 10);

        when(bookRepository.searchBooks("clean", pageable)).thenReturn(new PageImpl<>(List.of(book)));

        Page<BookResponse> response = bookService.getAllBooks("clean", pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().getTitle()).isEqualTo("Clean Code");
        assertThat(response.getContent().getFirst().getAvailableCopies()).isEqualTo(5);
    }

    @Test
    void shouldCreateBookWithAllCopiesAvailable() {
        BookRequest request = request("9780132350884", "Clean Code", "Robert C. Martin", 5);
        when(bookRepository.findByIsbn("9780132350884")).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            book.setId(3L);
            return book;
        });

        BookResponse response = bookService.createBook(request);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getIsbn()).isEqualTo("9780132350884");
        assertThat(response.getTotalCopies()).isEqualTo(5);
        assertThat(response.getAvailableCopies()).isEqualTo(5);
    }

    @Test
    void shouldRejectCreatingBookWithDuplicateIsbn() {
        when(bookRepository.findByIsbn("9780132350884")).thenReturn(Optional.of(new Book()));

        assertThatThrownBy(() -> bookService.createBook(request("9780132350884", "Clean Code", "Robert C. Martin", 5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Książka o podanym ISBN już istnieje w bazie.");

        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void shouldRejectUpdateWhenAvailableCopiesWouldBecomeNegative() {
        Book existing = book(1L, "9780132350884", "Clean Code", 1, 5);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(bookRepository.findByIsbn("9780132350884")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> bookService.updateBook(1L, request("9780132350884", "Clean Code", "Robert C. Martin", 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Zbyt mała liczba dostępnych egzemplarzy (książki w obiegu).");
    }

    @Test
    void shouldThrowWhenDeletingBookWithCopiesInCirculation() {
        Book existing = book(1L, "9780132350884", "Clean Code", 1, 5);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> bookService.deleteBook(1L))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessage("Nie można usunąć książki – egzemplarze są w obiegu!");
    }

    @Test
    void shouldThrowWhenBookDoesNotExist() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBookById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Nie znaleziono książki o ID: 99");
    }

    private BookRequest request(String isbn, String title, String author, int totalCopies) {
        BookRequest request = new BookRequest();
        request.setIsbn(isbn);
        request.setTitle(title);
        request.setAuthor(author);
        request.setPublisher("Prentice Hall");
        request.setPublicationYear(2008);
        request.setTotalCopies(totalCopies);
        return request;
    }

    private Book book(Long id, String isbn, String title, int availableCopies, int totalCopies) {
        Book book = new Book();
        book.setId(id);
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor("Robert C. Martin");
        book.setPublisher("Prentice Hall");
        book.setPublicationYear(2008);
        book.setAvailableCopies(availableCopies);
        book.setTotalCopies(totalCopies);
        return book;
    }
}
