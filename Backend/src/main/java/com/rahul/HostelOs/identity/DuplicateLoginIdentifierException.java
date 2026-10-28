package com.rahul.HostelOs.identity;

public class DuplicateLoginIdentifierException extends RuntimeException {

	public DuplicateLoginIdentifierException() {
		super("Login identifier already exists");
	}
}
