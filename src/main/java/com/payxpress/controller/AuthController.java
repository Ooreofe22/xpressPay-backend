package com.payxpress.controller;

import com.payxpress.service.OtpService;
import com.payxpress.service.SessionService;
import com.payxpress.enums.OtpPurpose;
import com.payxpress.model.User;
import com.payxpress.enums.UserStatus;
import com.payxpress.repository.UserRepository;
import com.payxpress.security.JwtService;
import com.payxpress.wallet.VirtualAccountService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern BVN_PATTERN = Pattern.compile("^\\d{11}$");

    private final OtpService otpService;
    private final SessionService sessionService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final VirtualAccountService virtualAccountService;

    @Value("${wallet.signup-bonus:5000}")
    private BigDecimal signupBonus;

    public AuthController(OtpService otpService, SessionService sessionService, JwtService jwtService,
                           UserRepository userRepository, VirtualAccountService virtualAccountService) {
        this.otpService = otpService;
        this.sessionService = sessionService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.virtualAccountService = virtualAccountService;
    }

    // Signup — single screen collects email + BVN. This sends the OTP; the BVN
    // is format-checked here but not persisted until verify-otp succeeds, so a
    // wrong/unverified email never leaves a half-created account behind.
    @PostMapping("/signup/request-otp")
    public ResponseEntity<?> requestSignupOtp(@RequestBody Map<String, String> body) {
        String email = normalizeEmail(body.get("email"));
        String bvn = body.get("bvn");

        if (bvn == null || !BVN_PATTERN.matcher(bvn).matches()) {
            return ResponseEntity.badRequest().body(Map.of("message", "BVN must be 11 digits"));
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already registered"));
        }

        otpService.generateAndSend(email, OtpPurpose.SIGNUP);
        return ResponseEntity.ok(Map.of("message", "OTP sent"));
    }

    // Signup — verify OTP. This is now the ONE step that finishes signup:
    // verifies the code, creates + activates the user, saves the BVN, seeds
    // the wallet bonus, generates virtual accounts, and issues tokens.
    @PostMapping("/signup/verify-otp")
    @Transactional
    public ResponseEntity<?> verifySignupOtp(@RequestBody Map<String, String> body,
                                              @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        String email = normalizeEmail(body.get("email"));
        String bvn = body.get("bvn");
        String otp = body.get("otp");

        if (bvn == null || !BVN_PATTERN.matcher(bvn).matches()) {
            return ResponseEntity.badRequest().body(Map.of("message", "BVN must be 11 digits"));
        }
        if (!otpService.verify(email, OtpPurpose.SIGNUP, otp)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid code"));
        }
        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Email already registered"));
        }

        User user = new User();
        user.setEmail(email);
        user.setBvn(bvn);
        user.setStatus(UserStatus.ACTIVE);
        user.setBalance(signupBonus);
        userRepository.save(user);

        virtualAccountService.generateAccountsForUser(user);

        return issueTokens(user, userAgent);
    }

    // Login - email screen, sends OTP
    @PostMapping("/login/request-otp")
    public ResponseEntity<?> requestLoginOtp(@RequestBody Map<String, String> body) {
        String email = normalizeEmail(body.get("email"));
        userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account found for this email"));
        otpService.generateAndSend(email, OtpPurpose.LOGIN);
        return ResponseEntity.ok(Map.of("message", "OTP sent"));
    }

    // Login - OTP screen, issues new tokens
    @PostMapping("/login/verify-otp")
    @Transactional
    public ResponseEntity<?> verifyLoginOtp(@RequestBody Map<String, String> body,
                                             @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        String email = normalizeEmail(body.get("email"));
        String otp = body.get("otp");

        if (!otpService.verify(email, OtpPurpose.LOGIN, otp)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid code"));
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return issueTokens(user, userAgent);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        var session = sessionService.validateRefreshToken(body.get("refreshToken"));
        String newAccessToken = jwtService.issueAccessToken(session.getUser());
        return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        sessionService.validateRefreshToken(body.get("refreshToken"));
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<?> issueTokens(User user, String deviceInfo) {
        String accessToken = jwtService.issueAccessToken(user);
        String refreshToken = sessionService.issueSession(user, deviceInfo);
        return ResponseEntity.ok(Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken,
                "userId", user.getId(),
                "email", user.getEmail(),
                "balance", user.getBalance()
        ));
    }

    private String normalizeEmail(String rawEmail) {
        if (rawEmail == null || !EMAIL_PATTERN.matcher(rawEmail.trim()).matches()) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        return rawEmail.trim().toLowerCase();
    }
}
