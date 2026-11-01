package com.rahul.HostelOs.enquiry.exception;

public class EnquiryVersionConflictException extends RuntimeException {

	public EnquiryVersionConflictException(Long enquiryId) {
		super("Enquiry has been modified by another request: " + enquiryId);
	}
}
