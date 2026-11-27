package cl.duoc.ep3.service;

import cl.duoc.ep3.dto.AccountResponse;
import cl.duoc.ep3.dto.AuthResponse;
import cl.duoc.ep3.dto.LoginRequest;
import cl.duoc.ep3.dto.RegisterRequest;
import cl.duoc.ep3.dto.UpdateProfileRequest;
import cl.duoc.ep3.model.Account;
import cl.duoc.ep3.model.UserRole;
import cl.duoc.ep3.repository.AccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.Period;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String adminRegistrationKey;

    public AuthService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${admin.registration-key:}") String adminRegistrationKey) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.adminRegistrationKey = adminRegistrationKey;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return createAuthResponse(createAccount(request, UserRole.CUSTOMER));
    }

    @Transactional
    public AuthResponse registerAdmin(RegisterRequest request, String invitationKey) {
        if (adminRegistrationKey.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "El registro de administradores no está habilitado.");
        }
        if (invitationKey == null || invitationKey.length() > 256
                || !MessageDigest.isEqual(
                        adminRegistrationKey.getBytes(StandardCharsets.UTF_8),
                        invitationKey.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La clave de invitación no es válida.");
        }
        if (request.password().length() < 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La contraseña de administrador debe tener al menos 12 caracteres.");
        }
        return createAuthResponse(createAccount(request, UserRole.ADMIN));
    }

    private Account createAccount(RegisterRequest request, UserRole role) {
        String email = request.correo().trim().toLowerCase();
        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        }
        validateAdultAndRut(request.birthDate(), request.rut());
        Account account = new Account(
                email,
                passwordEncoder.encode(request.password()),
                request.nombres().trim(),
                request.apellidoPaterno().trim(),
                request.apellidoMaterno().trim(),
                request.rut().trim().toUpperCase(),
                request.birthDate(),
                request.telefono().trim(),
                request.direccion().trim(),
                role == UserRole.CUSTOMER && (email.endsWith("@duoc.cl") || email.endsWith("@duocuc.cl")),
                role);
        if (role == UserRole.CUSTOMER) {
            String referralCode = request.codigoReferido() == null ? "" : request.codigoReferido().trim();
            if (!referralCode.isEmpty() && !RewardPolicy.REGISTRATION_CODE.equalsIgnoreCase(referralCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código de referido o regalo no es válido.");
            }
            if (RewardPolicy.REGISTRATION_CODE.equalsIgnoreCase(referralCode)) {
                account.addPoints(RewardPolicy.REGISTRATION_BONUS);
            }
        }
        return accountRepository.save(account);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Account account = accountRepository.findByEmailIgnoreCase(request.correo().trim())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos."));
        return createAuthResponse(account);
    }

    @Transactional
    public AccountResponse updateProfile(Account account, UpdateProfileRequest request) {
        validateRut(request.rut());
        account.updateProfile(
                request.nombres().trim(),
                request.apellidoPaterno().trim(),
                request.apellidoMaterno().trim(),
                request.rut().trim().toUpperCase(),
                request.telefono().trim(),
                request.direccion().trim());
        return AccountResponse.from(accountRepository.save(account));
    }

    private AuthResponse createAuthResponse(Account account) {
        JwtService.AuthToken token = jwtService.createToken(account);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), AccountResponse.from(account));
    }

    private void validateAdultAndRut(LocalDate birthDate, String rutValue) {
        if (birthDate == null || Period.between(birthDate, LocalDate.now()).getYears() < 18) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes tener 18 años o más para registrarte.");
        }
        validateRut(rutValue);
    }

    private void validateRut(String rutValue) {
        String rut = rutValue.trim().toUpperCase();
        String digits = rut.substring(0, rut.length() - 1);
        if (!digits.matches("\\d{7,8}") || !validRutCheckDigit(digits, rut.charAt(rut.length() - 1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El RUT no es válido.");
        }
    }

    private boolean validRutCheckDigit(String digits, char provided) {
        // Módulo 11 para comprobar el dígito verificador del RUT.
        int sum = 0;
        int multiplier = 2;
        for (int index = digits.length() - 1; index >= 0; index--) {
            sum += Character.digit(digits.charAt(index), 10) * multiplier;
            multiplier = multiplier == 7 ? 2 : multiplier + 1;
        }
        int remainder = 11 - (sum % 11);
        char expected = remainder == 11 ? '0' : remainder == 10 ? 'K' : (char) ('0' + remainder);
        return expected == provided;
    }
}
