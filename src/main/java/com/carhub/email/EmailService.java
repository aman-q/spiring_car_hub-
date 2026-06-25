package com.carhub.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Renders Thymeleaf templates and dispatches them via the active {@link EmailProvider}.
 *
 * <p>All methods are {@code @Async} and swallow+log failures, so a flaky mail provider
 * never fails or slows the originating request — an intentional improvement over the
 * Node service, which awaited (and could fail on) email sending inline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MS = 300;

    private final EmailProvider emailProvider;
    private final SpringTemplateEngine templateEngine;

    @Async
    public void sendOtp(String name, String toEmail, String otp, String subject) {
        Context context = new Context();
        context.setVariable("name", name);
        context.setVariable("otp", otp);
        dispatch(subject, toEmail, "email/otp-verification", context);
    }

    @Async
    public void sendBookingConfirmation(BookingEmailModel model, String subject) {
        dispatch(subject, model.recipientEmail(), "email/booking-confirmation", bookingContext(model));
    }

    @Async
    public void sendBookingCancellation(BookingEmailModel model, String subject) {
        dispatch(subject, model.recipientEmail(), "email/booking-cancellation", bookingContext(model));
    }

    @Async
    public void sendBookingCompletion(BookingEmailModel model, String subject) {
        dispatch(subject, model.recipientEmail(), "email/booking-completion", bookingContext(model));
    }

    private Context bookingContext(BookingEmailModel model) {
        Context context = new Context();
        context.setVariable("booking", model);
        return context;
    }

    private void dispatch(String subject, String toEmail, String template, Context context) {
        String html;
        try {
            html = templateEngine.process(template, context);
        } catch (Exception e) {
            // A template error is deterministic — retrying won't help.
            log.error("Failed to render '{}' email template for {}", template, toEmail, e);
            return;
        }
        sendWithRetry(subject, toEmail, html);
    }

    /**
     * Best-effort send with bounded exponential backoff. Transient provider/network
     * failures get a few retries; a final failure is logged (the request already
     * succeeded — email is fire-and-forget).
     */
    private void sendWithRetry(String subject, String toEmail, String html) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                emailProvider.send(subject, toEmail, html);
                return;
            } catch (Exception e) {
                if (attempt == MAX_ATTEMPTS) {
                    log.error("Failed to send '{}' email to {} after {} attempts", subject, toEmail, MAX_ATTEMPTS, e);
                    return;
                }
                long backoff = BASE_BACKOFF_MS * (1L << (attempt - 1));
                log.warn("Email send attempt {}/{} to {} failed ({}), retrying in {}ms",
                        attempt, MAX_ATTEMPTS, toEmail, e.getMessage(), backoff);
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }
}
