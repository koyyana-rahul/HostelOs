package com.rahul.HostelOs.common.exception;

import com.rahul.HostelOs.building.exception.BuildingArchivedException;
import com.rahul.HostelOs.building.exception.DuplicateBuildingCodeException;
import com.rahul.HostelOs.enquiry.exception.EnquiryNotFoundException;
import com.rahul.HostelOs.enquiry.exception.EnquiryVersionConflictException;
import com.rahul.HostelOs.enquiry.exception.InvalidEnquiryStatusTransitionException;
import com.rahul.HostelOs.listing.exception.DuplicateListingTitleException;
import com.rahul.HostelOs.listing.exception.InvalidListingStatusTransitionException;
import com.rahul.HostelOs.listing.exception.ListingNotFoundException;
import com.rahul.HostelOs.listing.exception.ListingValidationException;
import com.rahul.HostelOs.listing.exception.ListingVersionConflictException;
import com.rahul.HostelOs.inventory.exception.DuplicatePropertyCodeException;
import com.rahul.HostelOs.inventory.exception.InvalidPropertyStatusTransitionException;
import com.rahul.HostelOs.inventory.exception.PropertyNotFoundException;
import com.rahul.HostelOs.organization.exception.DuplicateOrganizationSlugException;
import com.rahul.HostelOs.organization.exception.InactiveUserAccountException;
import com.rahul.HostelOs.organization.exception.InvalidMembershipTransitionException;
import com.rahul.HostelOs.organization.exception.LastAdminProtectionException;
import com.rahul.HostelOs.organization.exception.MembershipAlreadyExistsException;
import com.rahul.HostelOs.organization.exception.MembershipNotFoundException;
import com.rahul.HostelOs.organization.exception.OrganizationAccessDeniedException;
import com.rahul.HostelOs.organization.exception.OrganizationNotFoundException;
import com.rahul.HostelOs.organization.exception.UserNotFoundException;
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

	@ExceptionHandler(PropertyNotFoundException.class)
	public ResponseEntity<ApiError> handlePropertyNotFound(
			PropertyNotFoundException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Property not found";
		ApiError error = ApiError.of(HttpStatus.NOT_FOUND, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(DuplicatePropertyCodeException.class)
	public ResponseEntity<ApiError> handleDuplicatePropertyCode(
			DuplicatePropertyCodeException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Duplicate property code";
		ApiError error = ApiError.of(HttpStatus.CONFLICT, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(InvalidPropertyStatusTransitionException.class)
	public ResponseEntity<ApiError> handleInvalidPropertyStatusTransition(
			InvalidPropertyStatusTransitionException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Invalid property status transition";
		ApiError error = ApiError.of(HttpStatus.CONFLICT, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(DuplicateBuildingCodeException.class)
	public ResponseEntity<ApiError> handleDuplicateBuildingCode(
			DuplicateBuildingCodeException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Duplicate building code";
		ApiError error = ApiError.of(HttpStatus.CONFLICT, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler({ListingNotFoundException.class, EnquiryNotFoundException.class})
	public ResponseEntity<ApiError> handleListingOrEnquiryNotFound(
			RuntimeException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Resource not found";
		ApiError error = ApiError.of(HttpStatus.NOT_FOUND, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(DuplicateListingTitleException.class)
	public ResponseEntity<ApiError> handleDuplicateListingTitle(
			DuplicateListingTitleException exception,
			HttpServletRequest request) {
		ApiError error = ApiError.of(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler({
			InvalidListingStatusTransitionException.class,
			ListingVersionConflictException.class,
			InvalidEnquiryStatusTransitionException.class,
			EnquiryVersionConflictException.class
	})
	public ResponseEntity<ApiError> handleListingOrEnquiryConflict(
			RuntimeException exception,
			HttpServletRequest request) {
		ApiError error = ApiError.of(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(ListingValidationException.class)
	public ResponseEntity<ApiError> handleListingValidation(
			ListingValidationException exception,
			HttpServletRequest request) {
		ApiError error = ApiError.of(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegalArgument(
			IllegalArgumentException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Invalid request";
		ApiError error = ApiError.of(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(OrganizationNotFoundException.class)
	public ResponseEntity<ApiError> handleOrganizationNotFound(
			OrganizationNotFoundException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Organization not found";
		ApiError error = ApiError.of(HttpStatus.NOT_FOUND, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(MembershipNotFoundException.class)
	public ResponseEntity<ApiError> handleMembershipNotFound(
			MembershipNotFoundException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Membership not found";
		ApiError error = ApiError.of(HttpStatus.NOT_FOUND, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiError> handleUserNotFound(
			UserNotFoundException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "User not found";
		ApiError error = ApiError.of(HttpStatus.NOT_FOUND, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(DuplicateOrganizationSlugException.class)
	public ResponseEntity<ApiError> handleDuplicateOrganizationSlug(
			DuplicateOrganizationSlugException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Duplicate organization slug";
		ApiError error = ApiError.of(HttpStatus.CONFLICT, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(MembershipAlreadyExistsException.class)
	public ResponseEntity<ApiError> handleMembershipAlreadyExists(
			MembershipAlreadyExistsException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Membership already exists";
		ApiError error = ApiError.of(HttpStatus.CONFLICT, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(InvalidMembershipTransitionException.class)
	public ResponseEntity<ApiError> handleInvalidMembershipTransition(
			InvalidMembershipTransitionException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Invalid membership transition";
		ApiError error = ApiError.of(HttpStatus.CONFLICT, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler(LastAdminProtectionException.class)
	public ResponseEntity<ApiError> handleLastAdminProtection(
			LastAdminProtectionException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Last active administrator cannot be removed or demoted";
		ApiError error = ApiError.of(HttpStatus.CONFLICT, message, request.getRequestURI());
		return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
	}

	@ExceptionHandler({OrganizationAccessDeniedException.class, InactiveUserAccountException.class})
	public ResponseEntity<ApiError> handleOrgAccessDenied(
			RuntimeException exception,
			HttpServletRequest request) {
		String message = (exception.getMessage() != null && !exception.getMessage().isBlank())
				? exception.getMessage()
				: "Access is denied";
		// Keep response generic per security but use exception message if meaningful; ApiError already standardized
		ApiError error = ApiError.of(HttpStatus.FORBIDDEN, "Access is denied", request.getRequestURI());
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
