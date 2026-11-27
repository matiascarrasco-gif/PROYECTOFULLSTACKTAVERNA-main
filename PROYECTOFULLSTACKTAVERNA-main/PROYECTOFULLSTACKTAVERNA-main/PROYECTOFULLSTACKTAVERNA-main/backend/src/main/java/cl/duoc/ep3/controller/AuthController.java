package cl.duoc.ep3.controller;

import cl.duoc.ep3.dto.AccountResponse;
import cl.duoc.ep3.dto.AuthResponse;
import cl.duoc.ep3.dto.LoginRequest;
import cl.duoc.ep3.dto.RegisterRequest;
import cl.duoc.ep3.dto.UpdateProfileRequest;
import cl.duoc.ep3.model.Account;
import cl.duoc.ep3.service.AuthService;
import cl.duoc.ep3.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(authService.register(request));
    }

    @PostMapping("/admin/register")
    public ResponseEntity<AuthResponse> registerAdmin(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "X-Admin-Registration-Key", required = false) String invitationKey) {
        return ResponseEntity.status(201).body(authService.registerAdmin(request, invitationKey));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public AccountResponse currentUser(@AuthenticationPrincipal Account account) {
        return AccountResponse.from(account);
    }

    @PutMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    public AccountResponse updateProfile(
            @AuthenticationPrincipal Account account,
            @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(account, request);
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            jwtService.revoke(authorization.substring(7).trim());
        }
        return ResponseEntity.noContent().build();
    }
}
