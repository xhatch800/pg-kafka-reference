package com.example.sample.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.sample.dto.request.CreateDepartmentRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundMapsTo404() {
        ProblemDetail problem = handler.handleNotFound(NotFoundException.of("Department", 42L));

        assertThat(problem.getStatus()).isEqualTo(404);
        assertThat(problem.getTitle()).isEqualTo("Not Found");
        assertThat(problem.getDetail()).isEqualTo("Department 42 not found");
    }

    @Test
    void conflictMapsTo409() {
        ProblemDetail problem = handler.handleConflict(new ConflictException("Employee 7 is already terminated"));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Conflict");
        assertThat(problem.getDetail()).isEqualTo("Employee 7 is already terminated");
    }

    @Test
    void dataIntegrityViolationMapsTo409() {
        ProblemDetail problem = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getTitle()).isEqualTo("Conflict");
        assertThat(problem.getDetail()).isEqualTo("The request conflicts with existing data.");
    }

    @Test
    void validationErrorsMapTo400WithFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "must not be blank"));
        bindingResult.addError(new FieldError("request", "location", "size must be between 0 and 100"));
        bindingResult.addError(new FieldError("request", "name", "second message is ignored"));

        ResponseEntity<Object> response = invokeValidationHandler(bindingResult);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getDetail()).isEqualTo("Validation failed");
        assertThat(problem.getProperties()).containsKey("errors");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors).containsExactly(
                Map.entry("name", "must not be blank"),
                Map.entry("location", "size must be between 0 and 100"));
    }

    @Test
    void validationErrorWithoutMessageFallsBackToInvalid() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", null, false, null, null, null));

        ResponseEntity<Object> response = invokeValidationHandler(bindingResult);

        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(problem).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) problem.getProperties().get("errors");
        assertThat(errors).containsEntry("name", "invalid");
    }

    private ResponseEntity<Object> invokeValidationHandler(BeanPropertyBindingResult bindingResult) throws Exception {
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("sampleEndpoint", CreateDepartmentRequest.class), 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);
        return handler.handleMethodArgumentNotValid(
                exception, new HttpHeaders(), HttpStatus.BAD_REQUEST,
                new ServletWebRequest(new MockHttpServletRequest()));
    }

    @SuppressWarnings("unused")
    private void sampleEndpoint(CreateDepartmentRequest request) {
    }

    @Test
    void unexpectedExceptionMapsTo500WithoutLeakingMessage() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("secret internal detail"));

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred.");
    }
}
