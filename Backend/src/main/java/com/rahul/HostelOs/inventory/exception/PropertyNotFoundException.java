package com.rahul.HostelOs.inventory.exception;

public class PropertyNotFoundException extends RuntimeException {

	public PropertyNotFoundException(String message) {
		super(message);
	}

	public PropertyNotFoundException(Long propertyId) {
		super("Property not found: " + propertyId);
	}

	public PropertyNotFoundException(Long propertyId, Long organizationId) {
		super("Property " + propertyId + " not found in organization " + organizationId);
	}
}
