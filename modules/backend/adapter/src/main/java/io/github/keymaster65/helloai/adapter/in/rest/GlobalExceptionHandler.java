package io.github.keymaster65.helloai.adapter.in.rest;

import io.github.keymaster65.helloai.application.service.RecipeNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Central translation of exceptions into RFC 9457 problem details (see docs/prompt/api.adoc).
 *
 * <p>Spring answers the exceptions of the framework itself – {@code 405}, {@code 415} and their
 * kind – once {@code spring.mvc.problemdetails.enabled} is set. Its handler is ordered at
 * {@code 0}, so this one has to claim the highest precedence to keep the four cases below,
 * including the typed field errors that Spring's payload does not carry.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(RecipeNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(
            RecipeNotFoundException ex, HttpServletRequest request) {
        return problem(ProblemType.NOT_FOUND, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ProblemDetail.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ProblemDetail.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        String detail = "%d field(s) of the request body are invalid".formatted(fieldErrors.size());
        return problem(ProblemType.VALIDATION_FAILED, detail, fieldErrors, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        // Deliberately not ex.getMessage(): it quotes the offending body and names Jackson's
        // internals (see docs/prompt/security.adoc, "Neues Fehlerbild").
        return problem(ProblemType.MALFORMED_REQUEST, "The request body could not be read as JSON",
                List.of(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        return problem(ProblemType.INVALID_ARGUMENT, ex.getMessage(), List.of(), request);
    }

    private static ResponseEntity<ProblemDetail> problem(
            ProblemType type,
            String detail,
            List<ProblemDetail.FieldError> fieldErrors,
            HttpServletRequest request) {

        ProblemDetail body = ProblemDetail.curried()
                .type(type.uri(request.getContextPath()))
                .title(type.title())
                .status(type.status().value())
                // An exception without a message must not turn `detail` into null: the contract
                // declares it as required.
                .detail(detail == null || detail.isBlank() ? type.title() : detail)
                .instance(request.getRequestURI())
                .fieldErrors(fieldErrors);

        return ResponseEntity.status(type.status())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
