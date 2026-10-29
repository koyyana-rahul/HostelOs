package com.rahul.HostelOs.organization.exception;

import com.rahul.HostelOs.organization.MembershipStatus;
import java.util.Optional;

public class MembershipAlreadyExistsException extends RuntimeException {

	private final MembershipStatus existingStatus;

	public MembershipAlreadyExistsException(MembershipStatus existingStatus) {
		super("User already has a " + existingStatus + " membership in this organization");
		this.existingStatus = existingStatus;
	}

	public MembershipAlreadyExistsException(String message) {
		super(message);
		this.existingStatus = null;
	}

	public Optional<MembershipStatus> getExistingStatus() {
		return Optional.ofNullable(existingStatus);
	}
}
