package com.rahul.HostelOs.building.exception;

public class DuplicateBuildingCodeException extends RuntimeException {

	public DuplicateBuildingCodeException(String message) {
		super(message);
	}

	public DuplicateBuildingCodeException(String code, Long organizationId) {
		super("Building code '" + code + "' already exists in organization " + organizationId);
	}
}
