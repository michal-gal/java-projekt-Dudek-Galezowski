package projekt.pk.edu.pl.bookstore.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import projekt.pk.edu.pl.bookstore.dto.auth.CurrentUserResponse;
import projekt.pk.edu.pl.bookstore.dto.auth.LoginRequest;
import projekt.pk.edu.pl.bookstore.dto.auth.LoginResponse;
import projekt.pk.edu.pl.bookstore.entity.Role;
import projekt.pk.edu.pl.bookstore.repository.UserRepository;
import projekt.pk.edu.pl.bookstore.security.JwtService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void shouldLoginWithNormalizedEmailAndReturnJwtResponse() {
        LoginRequest request = new LoginRequest(" USER@Example.com ", "superhaslo123");
        UserDetails userDetails = User.withUsername("user@example.com")
                .password("encoded")
                .authorities("ROLE_READER")
                .build();

        when(authenticationManager.authenticate(org.mockito.ArgumentMatchers.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authenticationService.login(request);

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor = ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        UsernamePasswordAuthenticationToken token = captor.getValue();

        assertThat(token.getPrincipal()).isEqualTo("user@example.com");
        assertThat(token.getCredentials()).isEqualTo("superhaslo123");
        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
    }

    @Test
    void shouldReturnCurrentUser() {
        Role role = new Role();
        role.setName("READER");

        projekt.pk.edu.pl.bookstore.entity.User user = new projekt.pk.edu.pl.bookstore.entity.User();
        user.setId(7L);
        user.setFirstName("Jan");
        user.setLastName("Kowalski");
        user.setEmail("jan@example.com");
        user.setRole(role);
        user.setIsActive(true);

        when(userRepository.findByEmail("jan@example.com")).thenReturn(Optional.of(user));

        CurrentUserResponse response = authenticationService.getCurrentUser("jan@example.com");

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.firstName()).isEqualTo("Jan");
        assertThat(response.lastName()).isEqualTo("Kowalski");
        assertThat(response.email()).isEqualTo("jan@example.com");
        assertThat(response.role()).isEqualTo("READER");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void shouldThrowWhenCurrentUserDoesNotExist() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authenticationService.getCurrentUser("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: missing@example.com");
    }
}
