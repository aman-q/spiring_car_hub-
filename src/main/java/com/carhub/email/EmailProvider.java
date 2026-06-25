package com.carhub.email;

/**
 * Strategy for delivering a rendered HTML email. Implementations are selected via
 * {@code carhub.email.provider} ({@link BrevoEmailProvider} or {@link SmtpEmailProvider}).
 */
public interface EmailProvider {

    void send(String subject, String toEmail, String htmlBody);
}
