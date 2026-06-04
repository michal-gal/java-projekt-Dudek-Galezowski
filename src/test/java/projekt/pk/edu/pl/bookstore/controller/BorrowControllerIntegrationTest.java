package projekt.pk.edu.pl.bookstore.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import projekt.pk.edu.pl.bookstore.dto.auth.LoginRequest;
import projekt.pk.edu.pl.bookstore.dto.auth.RegistrationRequest;
import projekt.pk.edu.pl.bookstore.entity.Book;
import projekt.pk.edu.pl.bookstore.entity.BorrowHistory;
import projekt.pk.edu.pl.bookstore.entity.BorrowStatus;
import projekt.pk.edu.pl.bookstore.entity.Reservation;
import projekt.pk.edu.pl.bookstore.entity.ReservationStatus;
import projekt.pk.edu.pl.bookstore.repository.BookRepository;
import projekt.pk.edu.pl.bookstore.repository.BorrowHistoryRepository;
import projekt.pk.edu.pl.bookstore.repository.ReservationRepository;
import projekt.pk.edu.pl.bookstore.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BorrowControllerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BorrowHistoryRepository borrowHistoryRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final List<String> createdEmails = new ArrayList<>();
    private final List<Long> createdBookIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @AfterEach
    void cleanup() {
        borrowHistoryRepository.deleteAll();
        reservationRepository.deleteAll();

        for (Long bookId : createdBookIds) {
            bookRepository.findById(bookId).ifPresent(bookRepository::delete);
        }
        createdBookIds.clear();

        for (String email : createdEmails) {
            userRepository.findByEmail(email).ifPresent(userRepository::delete);
        }
        createdEmails.clear();
    }

    @Test
    void shouldBorrowReservedBookAndUpdateAvailability() throws Exception {
        String email = registerReader();
        String accessToken = loginAndExtractToken(email, "superhaslo");
        Book book = createBook("Effective Java", 3, 2);
        Long reservationId = createReservation(accessToken, book.getId());

        mockMvc.perform(post("/api/borrows")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toBorrowJson(reservationId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(reservationId))
                .andExpect(jsonPath("$.bookId").value(book.getId()))
                .andExpect(jsonPath("$.bookTitle").value("Effective Java"))
                .andExpect(jsonPath("$.status").value("BORROWED"))
                .andExpect(jsonPath("$.borrowedAt").isString())
                .andExpect(jsonPath("$.dueAt").isString())
                .andExpect(jsonPath("$.returnedAt").isEmpty());

        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        Book updatedBook = bookRepository.findById(book.getId()).orElseThrow();
        BorrowHistory borrowHistory = borrowHistoryRepository.findAll().getFirst();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
        assertThat(reservation.getFulfilledAt()).isNotNull();
        assertThat(updatedBook.getAvailableCopies()).isEqualTo(1);
        assertThat(borrowHistory.getStatus()).isEqualTo(BorrowStatus.BORROWED);
        assertThat(borrowHistory.getReservation().getId()).isEqualTo(reservationId);
    }

    @Test
    void shouldReturnBorrowedBookAndRestoreAvailability() throws Exception {
        String email = registerReader();
        String accessToken = loginAndExtractToken(email, "superhaslo");
        Book book = createBook("Spring in Action", 2, 2);
        Long reservationId = createReservation(accessToken, book.getId());
        Long borrowId = borrowBook(accessToken, reservationId);

        mockMvc.perform(post("/api/borrows/%d/return".formatted(borrowId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(borrowId))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnedAt").isString());

        Book updatedBook = bookRepository.findById(book.getId()).orElseThrow();
        BorrowHistory borrowHistory = borrowHistoryRepository.findById(borrowId).orElseThrow();

        assertThat(updatedBook.getAvailableCopies()).isEqualTo(2);
        assertThat(borrowHistory.getStatus()).isEqualTo(BorrowStatus.RETURNED);
        assertThat(borrowHistory.getReturnedAt()).isNotNull();
    }

    @Test
    void shouldRejectBorrowingAnotherUsersReservation() throws Exception {
        String ownerEmail = registerReader();
        String ownerToken = loginAndExtractToken(ownerEmail, "superhaslo");
        String secondEmail = registerReader();
        String secondToken = loginAndExtractToken(secondEmail, "superhaslo");
        Book book = createBook("Patterns of Enterprise Application Architecture", 1, 1);
        Long reservationId = createReservation(ownerToken, book.getId());

        mockMvc.perform(post("/api/borrows")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toBorrowJson(reservationId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Rezerwacja nie należy do bieżącego użytkownika."));
    }

    private String registerReader() throws Exception {
        String email = uniqueEmail();
        RegistrationRequest request = new RegistrationRequest("Jan", "Kowalski", email, "superhaslo");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated());

        createdEmails.add(email);
        return email;
    }

    private Book createBook(String title, int totalCopies, int availableCopies) {
        Book book = new Book();
        book.setIsbn(UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        book.setTitle(title);
        book.setAuthor("Autor Testowy");
        book.setPublisher("Test Publisher");
        book.setPublicationYear(2024);
        book.setTotalCopies(totalCopies);
        book.setAvailableCopies(availableCopies);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());

        Book savedBook = bookRepository.saveAndFlush(book);
        createdBookIds.add(savedBook.getId());
        return savedBook;
    }

    private Long createReservation(String accessToken, Long bookId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toReservationJson(bookId)))
                .andExpect(status().isCreated())
                .andReturn();

        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)")
                .matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        return Long.parseLong(matcher.group(1));
    }

    private Long borrowBook(String accessToken, Long reservationId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/borrows")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toBorrowJson(reservationId)))
                .andExpect(status().isCreated())
                .andReturn();

        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)")
                .matcher(result.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        return Long.parseLong(matcher.group(1));
    }

    private String uniqueEmail() {
        return "borrow-" + UUID.randomUUID() + "@example.com";
    }

    private String toJson(RegistrationRequest request) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(request.firstName(), request.lastName(), request.email(), request.password());
    }

    private String toJson(LoginRequest request) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(request.email(), request.password());
    }

    private String toReservationJson(Long bookId) {
        return """
                {
                  "bookId": %d
                }
                """.formatted(bookId);
    }

    private String toBorrowJson(Long reservationId) {
        return """
                {
                  "reservationId": %d
                }
                """.formatted(reservationId);
    }

    private String loginAndExtractToken(String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        Matcher matcher = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(loginResult.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
