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
        try {
            String html = templateEngine.process(template, context);
            emailProvider.send(subject, toEmail, html);
        } catch (Exception e) {
            log.error("Failed to send '{}' email to {}", subject, toEmail, e);
        }
    }
}
