package com.rahul.HostelOs.enquiry.exception;

public class EnquiryNotFoundException extends RuntimeException {

	public EnquiryNotFoundException(Long enquiryId) {
		super("Enquiry not found: " + enquiryId);
	}
}
