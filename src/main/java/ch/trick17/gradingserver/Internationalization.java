package ch.trick17.gradingserver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Locale.LanguageRange;
import java.util.stream.Stream;

import static java.util.Locale.ENGLISH;
import static java.util.Objects.requireNonNullElse;
import static org.springframework.context.i18n.LocaleContextHolder.getLocale;

@Configuration
public class Internationalization implements WebMvcConfigurer {

    public static final Locale DEFAULT_LOCALE = ENGLISH;

    private final ResourceBundleMessageSource messageSource;
    private final List<Locale> supportedLocales;

    public Internationalization(ResourceBundleMessageSource messageSource,
                                @Value("classpath*:messages_*.properties") Resource[] messageFiles) {
        this.messageSource = messageSource;
        supportedLocales = Stream.of(messageFiles)
                .map(f -> f.getFilename().replaceAll("messages_|\\.properties", ""))
                .map(Locale::forLanguageTag)
                .toList();
    }

    @Bean
    public LocaleResolver localeResolver() {
        return new CookieLocaleResolver();
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        var interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    public DateTimeFormatter dateTimeFormatter(FormatStyle style) {
        return DateTimeFormatter
                .ofLocalizedDateTime(style)
                .withLocale(supportedLocale());
    }

    public String message(String key, Object... args) {
        return messageSource.getMessage(key, args, supportedLocale());
    }

    public Locale supportedLocale() {
        // make sure only locales that have a 'messages' file are used for
        // formatting times etc., to avoid partially localized text
        var priorities = LanguageRange.parse(getLocale().toLanguageTag());
        var bestMatch = Locale.lookup(priorities, supportedLocales);
        return requireNonNullElse(bestMatch, DEFAULT_LOCALE);
    }
}
