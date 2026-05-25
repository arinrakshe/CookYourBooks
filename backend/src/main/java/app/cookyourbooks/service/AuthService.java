package app.cookyourbooks.service;

import app.cookyourbooks.domain.User;
import app.cookyourbooks.dto.auth.AuthResponse;
import app.cookyourbooks.dto.auth.LoginRequest;
import app.cookyourbooks.dto.auth.RegisterRequest;
import app.cookyourbooks.exception.ConflictException;
import app.cookyourbooks.repository.UserRepository;
import app.cookyourbooks.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .displayName(request.displayName())
            .build();
        User saved = userRepository.save(user);
        return buildResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        } catch (BadCredentialsException ex) {
            throw new BadCredentialsException("Invalid email or password");
        }
        User user = userRepository.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        String token = jwtService.issueAccessToken(user.getId(), user.getEmail());
        return AuthResponse.builder()
            .token(token)
            .tokenType("Bearer")
            .user(AuthResponse.UserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build())
            .build();
    }
}
