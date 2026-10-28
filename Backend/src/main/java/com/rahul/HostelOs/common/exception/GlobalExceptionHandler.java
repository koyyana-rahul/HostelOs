package com.rahul.HostelOs.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		List<ApiError.FieldViolation> violations = new ArrayList<>();
		exception.getBindingResult().getFieldErrors().forEach(error ->
				violations.add(new ApiError.FieldViolation(error.getField(), error.getDefaultMessage())));
		exception.getBindingResult().getGlobalErrors().forEach(error ->
				violations.add(new ApiError.FieldViolation("", error.getDefaultMessage())));
		ApiError error = ApiError.validation("Validation failed", requestPath(request), violations);
		return new ResponseEntity<>(error, headers, status);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> handleConstraintViolation(
			ConstraintViolationException exception,
			HttpServletRequest request) {
		List<ApiError.FieldViolation> violations = exception.getConstraintViolations().stream()
				.map(this::toFieldViolation)
				.toList();
		ApiError error = ApiError.validation("Validation failed", request.getRequestURI(), violations);
		return ResponseEntity.badRequest().body(error);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
		ApiError error = ApiError.of(
				HttpStatus.FORBIDDEN,
				"Access is denied",
				request.getRequestURI());
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), exception);
		ApiError error = ApiError.of(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred",
				request.getRequestURI());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(
			Exception exception,
			Object body,
			HttpHeaders headers,
			HttpStatusCode status,
			WebRequest request) {
		HttpStatus httpStatus = HttpStatus.resolve(status.value());
		String message = httpStatus != null ? httpStatus.getReasonPhrase() : "Request could not be processed";
		ApiError error = ApiError.of(status.value(), message, requestPath(request));
		return new ResponseEntity<>(error, headers, status);
	}

	private ApiError.FieldViolation toFieldViolation(ConstraintViolation<?> violation) {
		return new ApiError.FieldViolation(violation.getPropertyPath().toString(), violation.getMessage());
	}

	private String requestPath(WebRequest request) {
		if (request instanceof ServletWebRequest servletRequest) {
			return servletRequest.getRequest().getRequestURI();
		}
		return "";
	}
}
