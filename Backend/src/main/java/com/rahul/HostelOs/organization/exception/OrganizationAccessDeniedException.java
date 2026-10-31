package com.rahul.HostelOs.organization.exception;

import org.springframework.security.access.AccessDeniedException;

public class OrganizationAccessDeniedException extends AccessDeniedException {

	public OrganizationAccessDeniedException(String message) {
		super(message);
	}
}
