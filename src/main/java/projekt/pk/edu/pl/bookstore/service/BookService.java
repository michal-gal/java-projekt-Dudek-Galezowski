package projekt.pk.edu.pl.bookstore.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import projekt.pk.edu.pl.bookstore.dto.book.BookRequest;
import projekt.pk.edu.pl.bookstore.dto.book.BookResponse;
import projekt.pk.edu.pl.bookstore.entity.Book;
import projekt.pk.edu.pl.bookstore.exception.BusinessConflictException;
import projekt.pk.edu.pl.bookstore.exception.ResourceNotFoundException;
import projekt.pk.edu.pl.bookstore.repository.BookRepository;

import java.time.LocalDateTime;

@Service
@Slf4j
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Page<BookResponse> getAllBooks(String query, Pageable pageable) {
        return bookRepository.searchBooks(query, pageable).map(this::mapToResponse);
    }

    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono książki o ID: " + id));
        return mapToResponse(book);
    }

    @Transactional
    public BookResponse createBook(BookRequest request) {
        log.info("Rozpoczęto proces dodawania nowej książki z ISBN: {}", request.getIsbn());

        if (bookRepository.findByIsbn(request.getIsbn()).isPresent()) {
            log.warn("Nieudana próba dodania książki - ISBN {} już istnieje", request.getIsbn());
            throw new IllegalArgumentException("Książka o podanym ISBN już istnieje w bazie.");
        }

        Book book = new Book();
        updateBookFields(book, request);
        book.setAvailableCopies(request.getTotalCopies());
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(bookRepository.save(book));
    }

    @Transactional
    public BookResponse updateBook(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono książki o ID: " + id));

        bookRepository.findByIsbn(request.getIsbn())
                .ifPresent(existingBook -> {
                    if (!existingBook.getId().equals(id)) {
                        throw new IllegalArgumentException("Inna książka posiada już ten ISBN.");
                    }
                });

        int diff = request.getTotalCopies() - book.getTotalCopies();
        int newAvailable = book.getAvailableCopies() + diff;
        if (newAvailable < 0) {
            throw new IllegalArgumentException("Zbyt mała liczba dostępnych egzemplarzy (książki w obiegu).");
        }

        updateBookFields(book, request);
        book.setAvailableCopies(newAvailable);
        book.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Long id) {
        log.info("Otrzymano żądanie usunięcia książki o ID: {}", id);

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono książki o ID: " + id));

        if (book.getAvailableCopies() < book.getTotalCopies()) {
            log.error("Zablokowano usunięcie książki ID: {} - egzemplarze są aktualnie w obiegu", id);
            throw new BusinessConflictException("Nie można usunąć książki – egzemplarze są w obiegu!");
        }
        bookRepository.delete(book);
        log.info("Książka o ID: {} została pomyślnie usunięta", id);
    }

    private void updateBookFields(Book book, BookRequest request) {
        book.setIsbn(request.getIsbn());
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setPublisher(request.getPublisher());
        book.setPublicationYear(request.getPublicationYear());
        book.setTotalCopies(request.getTotalCopies());
    }

    private BookResponse mapToResponse(Book book) {
        BookResponse response = new BookResponse();
        response.setId(book.getId());
        response.setIsbn(book.getIsbn());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setPublisher(book.getPublisher());
        response.setPublicationYear(book.getPublicationYear());
        response.setTotalCopies(book.getTotalCopies());
        response.setAvailableCopies(book.getAvailableCopies());
        response.setCreatedAt(book.getCreatedAt());
        response.setUpdatedAt(book.getUpdatedAt());
        return response;
    }
}