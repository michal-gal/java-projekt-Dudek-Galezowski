package projekt.pk.edu.pl.bookstore.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import projekt.pk.edu.pl.bookstore.dto.auth.LoginRequest;
import projekt.pk.edu.pl.bookstore.dto.auth.RegistrationRequest;
import projekt.pk.edu.pl.bookstore.entity.User;
import projekt.pk.edu.pl.bookstore.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthControllerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private final List<String> createdEmails = new ArrayList<>();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @AfterEach
    void cleanup() {
        for (String email : createdEmails) {
            userRepository.findByEmail(email).ifPresent(userRepository::delete);
        }
        createdEmails.clear();
    }

    @Test
    void shouldRegisterUserWithReaderRole() throws Exception {
        String email = uniqueEmail();
        RegistrationRequest request = new RegistrationRequest("Jan", "Kowalski", email.toUpperCase(), "superhaslo");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("READER"));

        createdEmails.add(email);

        User savedUser = userRepository.findByEmail(email).orElseThrow();
        assertThat(savedUser.getFirstName()).isEqualTo("Jan");
        assertThat(savedUser.getLastName()).isEqualTo("Kowalski");
        assertThat(savedUser.getRole().getName()).isEqualTo("READER");
        assertThat(savedUser.getIsActive()).isTrue();
        assertThat(savedUser.getPasswordHash()).startsWith("$2");
        assertThat(savedUser.getPasswordHash()).isNotEqualTo("superhaslo");
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        RegistrationRequest request = new RegistrationRequest("Anna", "Nowak", email, "superhaslo");
        RegistrationRequest duplicateRequest = new RegistrationRequest("Anna", "Nowak", email.toUpperCase(), "superhaslo");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated());

        createdEmails.add(email);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already in use: " + email));
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        RegistrationRequest request = new RegistrationRequest("", "Nowak", "not-an-email", "123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.firstName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void shouldLoginRegisteredUserAndReturnJwtToken() throws Exception {
        String email = uniqueEmail();
        RegistrationRequest registrationRequest = new RegistrationRequest("Jan", "Kowalski", email, "superhaslo");
        LoginRequest loginRequest = new LoginRequest(email.toUpperCase(), "superhaslo");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registrationRequest)))
                .andExpect(status().isCreated());

        createdEmails.add(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void shouldRejectLoginWithInvalidCredentials() throws Exception {
        String email = uniqueEmail();
        RegistrationRequest registrationRequest = new RegistrationRequest("Jan", "Kowalski", email, "superhaslo");
        LoginRequest loginRequest = new LoginRequest(email, "zlehaslo");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registrationRequest)))
                .andExpect(status().isCreated());

        createdEmails.add(email);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void shouldReturnCurrentUserForValidJwtToken() throws Exception {
        String email = uniqueEmail();
        RegistrationRequest registrationRequest = new RegistrationRequest("Jan", "Kowalski", email.toUpperCase(), "superhaslo");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(registrationRequest)))
                .andExpect(status().isCreated());

        createdEmails.add(email);

        String accessToken = loginAndExtractToken(email.toUpperCase(), "superhaslo");

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Jan"))
                .andExpect(jsonPath("$.lastName").value("Kowalski"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("READER"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    void shouldRejectCurrentUserRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
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
}
