package mn.demo.mini_bank.adapter.in.web;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import mn.demo.mini_bank.application.service.RegisterService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterService registerService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final SessionAuthenticationStrategy sessionStrategy;

    public AuthController(
            RegisterService registerService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository contextRepository,
            SessionAuthenticationStrategy sessionStrategy) {

        this.registerService = registerService;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.sessionStrategy = sessionStrategy;
    }

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterService.RegistrationResult register(
            @Valid @RequestBody RegisterRequest request) {

        return registerService.register(
                request.fullName(),
                request.username(),
                request.password(),
                request.confirmPassword()
        );
    }

    @PostMapping("/login")
    public Map<String, String> login(
            @Valid @RequestBody LoginRequest body,
            HttpServletRequest request,
            HttpServletResponse response) {

        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        body.username(),
                        body.password()
                )
        );

        request.getSession(true);

        sessionStrategy.onAuthentication(
                authentication,
                request,
                response
        );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        contextRepository.saveContext(
                context,
                request,
                response
        );

        return Map.of(
                "username", authentication.getName(),
                "message", "Login successful"
        );
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        return Map.of(
                "username", authentication.getName()
        );
    }

    public record RegisterRequest(
            @NotBlank
            @Size(min = 2, max = 120)
            String fullName,

            @NotBlank
            @Pattern(regexp = "[A-Za-z0-9_]{3,50}")
            String username,

            @NotBlank
            @Size(min = 8, max = 72)
            String password,

            @NotBlank
            String confirmPassword
    ) {}

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {}

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, String> authenticationError() {
        return Map.of(
                "error", "Invalid username or password"
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(Exception ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(Exception ex) {
        return Map.of("error", ex.getMessage());
    }
}
