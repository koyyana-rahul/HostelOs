package com.rahul.HostelOs.common.exception;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

public record ApiError(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		List<FieldViolation> violations) {

	public ApiError {
		violations = violations == null ? List.of() : List.copyOf(violations);
	}

	public static ApiError of(HttpStatus status, String message, String path) {
		return of(status.value(), message, path);
	}

	public static ApiError of(int status, String message, String path) {
		HttpStatus httpStatus = HttpStatus.resolve(status);
		String reason = httpStatus != null ? httpStatus.getReasonPhrase() : "HTTP " + status;
		return new ApiError(Instant.now(), status, reason, message, path, List.of());
	}

	public static ApiError validation(String message, String path, List<FieldViolation> violations) {
		return new ApiError(
				Instant.now(),
				HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				message,
				path,
				violations);
	}

	public record FieldViolation(String field, String message) {
	}
}
