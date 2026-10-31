package com.rahul.HostelOs.inventory.exception;

import com.rahul.HostelOs.inventory.PropertyStatus;

public class InvalidPropertyStatusTransitionException extends RuntimeException {

	public InvalidPropertyStatusTransitionException(String message) {
		super(message);
	}

	public InvalidPropertyStatusTransitionException(PropertyStatus currentStatus, PropertyStatus targetStatus) {
		super("Cannot transition property status from " + currentStatus + " to " + targetStatus);
	}
}
