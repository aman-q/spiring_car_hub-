package com.carhub.email;

import com.carhub.config.properties.BrevoProperties;
import com.carhub.config.properties.EmailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Sends mail through Brevo's transactional email API — the active provider, mirroring
 * the Node {@code config/email.js}. Selected when {@code carhub.email.provider=brevo}
 * (the default).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "carhub.email.provider", havingValue = "brevo", matchIfMissing = true)
public class BrevoEmailProvider implements EmailProvider {

    private final BrevoProperties brevoProperties;
    private final EmailProperties emailProperties;
    private final RestClient restClient;

    public BrevoEmailProvider(BrevoProperties brevoProperties, EmailProperties emailProperties) {
        this.brevoProperties = brevoProperties;
        this.emailProperties = emailProperties;
        this.restClient = RestClient.builder().baseUrl(brevoProperties.baseUrl()).build();
    }

    @Override
    public void send(String subject, String toEmail, String htmlBody) {
        Map<String, Object> payload = Map.of(
                "sender", Map.of("name", emailProperties.senderName(), "email", emailProperties.senderEmail()),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "htmlContent", htmlBody);

        restClient.post()
                .uri("/smtp/email")
                .header("api-key", brevoProperties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

        log.debug("Brevo email dispatched to {}", toEmail);
    }
}
