package com.rahul.HostelOs.organization.exception;

public class DuplicateOrganizationSlugException extends RuntimeException {

	public DuplicateOrganizationSlugException(String message) {
		super(message);
	}

	public DuplicateOrganizationSlugException() {
		super("Organization slug already exists");
	}
}
