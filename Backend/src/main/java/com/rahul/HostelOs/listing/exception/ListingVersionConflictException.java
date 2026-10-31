package com.rahul.HostelOs.listing.exception;

public class ListingVersionConflictException extends RuntimeException {

	public ListingVersionConflictException(Long listingId) {
		super("Listing has been modified by another request: " + listingId);
	}
}
