package projekt.pk.edu.pl.bookstore.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import projekt.pk.edu.pl.bookstore.entity.Book;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSearchBooksByTitleAuthorOrIsbnIgnoringCase() {
        Book cleanCode = createBook("9780132350884", "Clean Code", "Robert C. Martin");
        createBook("9780321125217", "Domain-Driven Design", "Eric Evans");

        Page<Book> resultByTitle = bookRepository.searchBooks("clean", PageRequest.of(0, 10));
        Page<Book> resultByAuthor = bookRepository.searchBooks("martin", PageRequest.of(0, 10));
        Page<Book> resultByIsbn = bookRepository.searchBooks("013235", PageRequest.of(0, 10));

        assertThat(resultByTitle.getContent()).extracting(Book::getId).containsExactly(cleanCode.getId());
        assertThat(resultByAuthor.getContent()).extracting(Book::getId).containsExactly(cleanCode.getId());
        assertThat(resultByIsbn.getContent()).extracting(Book::getId).containsExactly(cleanCode.getId());
    }

    @Test
    void shouldReturnAllBooksWhenSearchQueryIsNull() {
        Book firstBook = createBook(uniqueIsbn(), "Refactoring", "Martin Fowler");
        Book secondBook = createBook(uniqueIsbn(), "Effective Java", "Joshua Bloch");

        Page<Book> result = bookRepository.searchBooks(null, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Book::getId)
                .contains(firstBook.getId(), secondBook.getId());
    }

    @Test
    void shouldFindBookForUpdateById() {
        Book book = createBook(uniqueIsbn(), "Working Effectively with Legacy Code", "Michael Feathers");

        Book foundBook = bookRepository.findByIdForUpdate(book.getId()).orElseThrow();

        assertThat(foundBook.getId()).isEqualTo(book.getId());
        assertThat(foundBook.getTitle()).isEqualTo("Working Effectively with Legacy Code");
    }

    private Book createBook(String isbn, String title, String author) {
        Book book = new Book();
        LocalDateTime now = LocalDateTime.now();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        book.setPublisher("Test Publisher");
        book.setPublicationYear(2024);
        book.setTotalCopies(5);
        book.setAvailableCopies(5);
        book.setCreatedAt(now);
        book.setUpdatedAt(now);
        entityManager.persist(book);
        entityManager.flush();
        return book;
    }

    private String uniqueIsbn() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 13);
    }
}
