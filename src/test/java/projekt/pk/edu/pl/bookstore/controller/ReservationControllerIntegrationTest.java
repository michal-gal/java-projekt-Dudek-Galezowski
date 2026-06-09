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
import projekt.pk.edu.pl.bookstore.repository.BookRepository;
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
class ReservationControllerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ReservationRepository reservationRepository;

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
    void shouldCreateReservationForAuthenticatedReader() throws Exception {
        String email = registerReader();
        String accessToken = loginAndExtractToken(email, "superhaslo");
        Book book = createBook("Clean Code", 4, 2);

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toReservationJson(book.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.bookId").value(book.getId()))
                .andExpect(jsonPath("$.bookTitle").value("Clean Code"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.reservedAt").isString())
                .andExpect(jsonPath("$.expiresAt").isString());

        assertThat(reservationRepository.findAll()).hasSize(1);
        assertThat(reservationRepository.findAll().getFirst().getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void shouldRejectReservationWhenNoCopiesAreAvailable() throws Exception {
        String email = registerReader();
        String accessToken = loginAndExtractToken(email, "superhaslo");
        Book book = createBook("Domain-Driven Design", 3, 0);

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toReservationJson(book.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Brak dostępnych egzemplarzy książki o ID: " + book.getId()));
    }

    @Test
    void shouldRejectSecondActiveReservationForSameBook() throws Exception {
        String email = registerReader();
        String accessToken = loginAndExtractToken(email, "superhaslo");
        Book book = createBook("Refactoring", 5, 1);

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toReservationJson(book.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reservations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toReservationJson(book.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Użytkownik posiada już aktywną rezerwację dla książki o ID: " + book.getId()));
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

    private String uniqueEmail() {
        return "reservation-" + UUID.randomUUID() + "@example.com";
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
