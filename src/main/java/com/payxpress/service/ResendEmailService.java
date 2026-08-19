package com.payxpress.service;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Map;

/**
 * Sends real OTP emails via the Resend API (https://resend.com). Active only
 * under the prod profile so local/dev/test never fire real emails.
 *
 * Required env vars:
 *   RESEND_API_KEY   - Resend API key
 *   RESEND_FROM      - verified sender, e.g. "PayXpress <auth@yourdomain.com>"
 */
@Service
@Profile("prod")
public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final WebClient webClient;
    private final String apiKey;
    private final String from;

    public ResendEmailService(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from}") String from) {
        this.apiKey = apiKey;
        this.from = from;

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .responseTimeout(Duration.ofSeconds(10))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10))
                        .addHandlerLast(new WriteTimeoutHandler(10)));

        this.webClient = WebClient.builder()
                .baseUrl(RESEND_API_URL)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Override
    public void sendOtp(String email, String code) {
        String html = "<div style=\"font-family:sans-serif;padding:24px\">"
                + "<p>Your PayXpress verification code is:</p>"
                + "<p style=\"font-size:32px;font-weight:700;letter-spacing:4px\">" + code + "</p>"
                + "<p style=\"color:#66736c;font-size:13px\">This code expires in 5 minutes. "
                + "If you didn't request this, you can ignore this email.</p>"
                + "</div>";

        try {
            webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(Map.of(
                            "from", from,
                            "to", email,
                            "subject", "Your PayXpress verification code",
                            "html", html
                    ))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.error("Failed to send OTP email via Resend to {}", email, e);
            throw new IllegalStateException("Could not send verification email. Please try again.");
        }
    }
}