package com.carhub.common.message;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

/**
 * Thin wrapper over Spring's {@link MessageSource}. Resolves a key from
 * {@code messages.properties} for the current locale; if the key is missing it
 * returns the key itself rather than throwing, so a typo never 500s a request.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    public String get(String key, Object... args) {
        return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }
}
