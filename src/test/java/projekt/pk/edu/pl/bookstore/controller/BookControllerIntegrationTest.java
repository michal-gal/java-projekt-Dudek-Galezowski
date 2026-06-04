package projekt.pk.edu.pl.bookstore.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import projekt.pk.edu.pl.bookstore.dto.auth.LoginRequest;
import projekt.pk.edu.pl.bookstore.dto.auth.RegistrationRequest;
import projekt.pk.edu.pl.bookstore.entity.Book;
import projekt.pk.edu.pl.bookstore.entity.Role;
import projekt.pk.edu.pl.bookstore.entity.User;
import projekt.pk.edu.pl.bookstore.repository.BookRepository;
import projekt.pk.edu.pl.bookstore.repository.RoleRepository;
import projekt.pk.edu.pl.bookstore.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class BookControllerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void shouldRequireAuthenticationForBookList() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnFilteredBooksForAuthenticatedReader() throws Exception {
        String readerEmail = registerReader();
        String accessToken = loginAndExtractToken(readerEmail, "superhaslo");
        createBook("Clean Code", "9780132350884", 5, 5);
        createBook("Domain-Driven Design", "9780321125217", 3, 3);

        mockMvc.perform(get("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("search", "clean")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Clean Code"))
                .andExpect(jsonPath("$.content[0].isbn").value("9780132350884"));
    }

    @Test
    void shouldReturnBookByIdForAuthenticatedReader() throws Exception {
        String readerEmail = registerReader();
        String accessToken = loginAndExtractToken(readerEmail, "superhaslo");
        Book book = createBook("Effective Java", "9780134685991", 4, 4);

        mockMvc.perform(get("/api/books/" + book.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(book.getId()))
                .andExpect(jsonPath("$.title").value("Effective Java"))
                .andExpect(jsonPath("$.author").value("Autor Testowy"))
                .andExpect(jsonPath("$.availableCopies").value(4));
    }

    @Test
    void shouldCreateBookForAdmin() throws Exception {
        String adminEmail = createUserWithRole("ADMIN");
        String accessToken = loginAndExtractToken(adminEmail, "superhaslo");

        MvcResult result = mockMvc.perform(post("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("9780132350884", "Clean Code", 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isbn").value("9780132350884"))
                .andExpect(jsonPath("$.title").value("Clean Code"))
                .andExpect(jsonPath("$.availableCopies").value(5))
                .andReturn();

        Long createdBookId = extractId(result.getResponse().getContentAsString());
        createdBookIds.add(createdBookId);
        Book savedBook = bookRepository.findById(createdBookId).orElseThrow();
        assertThat(savedBook.getTitle()).isEqualTo("Clean Code");
    }

    @Test
    void shouldRejectBookCreationForReader() throws Exception {
        String readerEmail = registerReader();
        String accessToken = loginAndExtractToken(readerEmail, "superhaslo");

        mockMvc.perform(post("/api/books")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("9780132350884", "Clean Code", 5)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldUpdateBookForAdmin() throws Exception {
        String adminEmail = createUserWithRole("ADMIN");
        String accessToken = loginAndExtractToken(adminEmail, "superhaslo");
        Book book = createBook("Clean Code", "9780132350884", 5, 5);

        mockMvc.perform(put("/api/books/" + book.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookJson("9780132350884", "Clean Code Updated", 7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean Code Updated"))
                .andExpect(jsonPath("$.totalCopies").value(7))
                .andExpect(jsonPath("$.availableCopies").value(7));

        Book updatedBook = bookRepository.findById(book.getId()).orElseThrow();
        assertThat(updatedBook.getTitle()).isEqualTo("Clean Code Updated");
        assertThat(updatedBook.getAvailableCopies()).isEqualTo(7);
    }

    @Test
    void shouldDeleteBookForAdminWhenNoCopiesInCirculation() throws Exception {
        String adminEmail = createUserWithRole("ADMIN");
        String accessToken = loginAndExtractToken(adminEmail, "superhaslo");
        Book book = createBook("Clean Code", "9780132350884", 5, 5);

        mockMvc.perform(delete("/api/books/" + book.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        createdBookIds.remove(book.getId());
        assertThat(bookRepository.findById(book.getId())).isEmpty();
    }

    @Test
    void shouldReturnConflictWhenDeletingBookInCirculation() throws Exception {
        String adminEmail = createUserWithRole("ADMIN");
        String accessToken = loginAndExtractToken(adminEmail, "superhaslo");
        Book book = createBook("Clean Code", "9780132350884", 2, 5);

        mockMvc.perform(delete("/api/books/" + book.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Nie można usunąć książki – egzemplarze są w obiegu!"));
    }

    private String registerReader() throws Exception {
        String email = uniqueEmail("reader");
        RegistrationRequest request = new RegistrationRequest("Jan", "Kowalski", email, "superhaslo");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated());

        createdEmails.add(email);
        return email;
    }

    private String createUserWithRole(String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();

        User user = new User();
        String email = uniqueEmail(roleName.toLowerCase());
        user.setFirstName("Admin");
        user.setLastName("Test");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode("superhaslo"));
        user.setRole(role);
        user.setIsActive(true);

        userRepository.saveAndFlush(user);
        createdEmails.add(email);
        return email;
    }

    private Book createBook(String title, String isbn, int availableCopies, int totalCopies) {
        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor("Autor Testowy");
        book.setPublisher("Test Publisher");
        book.setPublicationYear(2024);
        book.setAvailableCopies(availableCopies);
        book.setTotalCopies(totalCopies);
        book.setCreatedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());

        Book savedBook = bookRepository.saveAndFlush(book);
        createdBookIds.add(savedBook.getId());
        return savedBook;
    }

    private String bookJson(String isbn, String title, int totalCopies) {
        return """
                {
                  "isbn": "%s",
                  "title": "%s",
                  "author": "Robert C. Martin",
                  "publisher": "Prentice Hall",
                  "publicationYear": 2008,
                  "totalCopies": %d
                }
                """.formatted(isbn, title, totalCopies);
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

    private Long extractId(String json) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(json);
        assertThat(matcher.find()).isTrue();
        return Long.parseLong(matcher.group(1));
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
