package com.oneff.customer.rest;

import com.oneff.customer.core.exception.CustomerValidationException;
import com.oneff.customer.core.exception.DocumentValidationException;
import com.oneff.customer.core.exception.DuplicateDocumentException;
import com.oneff.customer.core.exception.InvalidStatusTransitionException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Locale;

/**
 * Scoped exception handler for the Customer Registry REST API.
 *
 * <p>Translates domain exceptions into RFC 9457 ProblemDetail responses
 * with i18n support via the library's own {@code customerRegistryMessageSource}.</p>
 */
@RestControllerAdvice(basePackageClasses = CustomerController.class)
class CustomerExceptionHandler {

    private final MessageSource messageSource;

    CustomerExceptionHandler(
            @Qualifier("customerRegistryMessageSource") MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(DocumentValidationException.class)
    ProblemDetail handleDocumentValidation(DocumentValidationException ex) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
            "customer.registry.error.document.invalid", null, ex.getMessage(), locale);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, message);
        problem.setTitle("Document Validation Error");
        return problem;
    }

    @ExceptionHandler(DuplicateDocumentException.class)
    ProblemDetail handleDuplicateDocument(DuplicateDocumentException ex) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
            "customer.registry.error.document.duplicate", null, ex.getMessage(), locale);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT, message);
        problem.setTitle("Duplicate Document");
        return problem;
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    ProblemDetail handleInvalidTransition(InvalidStatusTransitionException ex) {
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
            "customer.registry.error.status.transition", null, ex.getMessage(), locale);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_ENTITY, message);
        problem.setTitle("Invalid Status Transition");
        return problem;
    }

    @ExceptionHandler(CustomerValidationException.class)
    ProblemDetail handleValidation(CustomerValidationException ex) {
        Locale locale = LocaleContextHolder.getLocale();
        String title = messageSource.getMessage(
            "customer.registry.error.validation.failed", null, "Validation failed", locale);

        List<String> resolvedErrors = ex.getErrorKeys().stream()
            .map(key -> messageSource.getMessage(key, null, key, locale))
            .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, String.join("; ", resolvedErrors));
        problem.setTitle(title);
        problem.setProperty("errors", resolvedErrors);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, String.join("; ", errors));
        problem.setTitle("Validation Error");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        Locale locale = LocaleContextHolder.getLocale();

        // Check if this is a "Customer not found" error from the service
        if (ex.getMessage() != null && ex.getMessage().contains("Customer not found")) {
            String message = messageSource.getMessage(
                "customer.registry.error.customer.not_found", null, ex.getMessage(), locale);
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, message);
            problem.setTitle("Customer Not Found");
            return problem;
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        return problem;
    }
}
