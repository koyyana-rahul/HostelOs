package com.rahul.HostelOs.inventory.exception;

public class DuplicatePropertyCodeException extends RuntimeException {

	public DuplicatePropertyCodeException(String message) {
		super(message);
	}

	public DuplicatePropertyCodeException(String code, Long organizationId) {
		super("Property code '" + code + "' already exists in organization " + organizationId);
	}
}
