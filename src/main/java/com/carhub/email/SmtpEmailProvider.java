package com.carhub.email;

import com.carhub.config.properties.EmailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;

/**
 * SMTP delivery via {@link JavaMailSender} (e.g. Gmail). Selected when
 * {@code carhub.email.provider=smtp}. Demonstrates the swappable Strategy: no other
 * code changes when the provider is switched.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "carhub.email.provider", havingValue = "smtp")
@RequiredArgsConstructor
public class SmtpEmailProvider implements EmailProvider {

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    @Override
    public void send(String subject, String toEmail, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(emailProperties.senderEmail(), emailProperties.senderName());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.debug("SMTP email dispatched to {}", toEmail);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to send SMTP email to " + toEmail, e);
        }
    }
}
