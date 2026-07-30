package com.payxpress.auth;

import com.payxpress.model.OtpPurpose;
import com.payxpress.model.User;
import com.payxpress.model.UserStatus;
import com.payxpress.repository.UserRepository;
import com.payxpress.security.JwtService;
import com.payxpress.wallet.VirtualAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final OtpService otpService;
    private final SessionService sessionService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final VirtualAccountService virtualAccountService;

    public AuthController(OtpService otpService, SessionService sessionService, JwtService jwtService,
                           UserRepository userRepository, VirtualAccountService virtualAccountService) {
        this.otpService = otpService;
        this.sessionService = sessionService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.virtualAccountService = virtualAccountService;
    }

    // Step 1 of signup — BVN screen (format-check only, no real BVN verification)
    @PostMapping("/signup/bvn")
    public ResponseEntity<?> submitBvn(@RequestBody Map<String, String> body) {
        String bvn = body.get("bvn");
        if (bvn == null || !bvn.matches("\\d{11}")) {
            return ResponseEntity.badRequest().body(Map.of("message", "BVN must be 11 digits"));
        }
        return ResponseEntity.ok(Map.of("valid", true));
    }

    // Step 2 — phone screen, sends OTP
    @PostMapping("/signup/request-otp")
    public ResponseEntity<?> requestSignupOtp(@RequestBody Map<String, String> body) {
        String phone = normalizePhone(body.get("phone"));
        if (userRepository.existsByPhone(phone)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Phone already registered"));
        }
        otpService.generateAndSend(phone, OtpPurpose.SIGNUP);
        return ResponseEntity.ok(Map.of("message", "OTP sent"));
    }

    // Step 3 — OTP screen, creates the user + virtual accounts on success
    @PostMapping("/signup/verify-otp")
    @Transactional
    public ResponseEntity<?> verifySignupOtp(@RequestBody Map<String, String> body,
                                              @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        String phone = normalizePhone(body.get("phone"));
        String bvn = body.get("bvn");
        String otp = body.get("otp");

        if (!otpService.verify(phone, OtpPurpose.SIGNUP, otp)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid code"));
        }

        User user = new User();
        user.setPhone(phone);
        user.setBvn(bvn);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        virtualAccountService.generateAccountsForUser(user);

        return issueTokens(user, userAgent);
    }

    // Login — phone screen, sends OTP
    @PostMapping("/login/request-otp")
    public ResponseEntity<?> requestLoginOtp(@RequestBody Map<String, String> body) {
        String phone = normalizePhone(body.get("phone"));
        userRepository.findByPhone(phone)
                .orElseThrow(() -> new IllegalArgumentException("No account found for this number"));
        otpService.generateAndSend(phone, OtpPurpose.LOGIN);
        return ResponseEntity.ok(Map.of("message", "OTP sent"));
    }

    // Login — OTP screen, revokes previous session, issues new tokens
    @PostMapping("/login/verify-otp")
    @Transactional
    public ResponseEntity<?> verifyLoginOtp(@RequestBody Map<String, String> body,
                                             @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        String phone = normalizePhone(body.get("phone"));
        String otp = body.get("otp");

        if (!otpService.verify(phone, OtpPurpose.LOGIN, otp)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid code"));
        }

        User user = userRepository.findByPhone(phone)
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
                "phone", user.getPhone()
        ));
    }

    private String normalizePhone(String rawPhone) {
        String digits = rawPhone.replaceAll("\\D", "");
        if (digits.startsWith("0")) digits = digits.substring(1);
        return digits.startsWith("234") ? "+" + digits : "+234" + digits;
    }
}
