package com.rahul.HostelOs.listing.exception;

public class ListingNotFoundException extends RuntimeException {

	public ListingNotFoundException(Long listingId) {
		super("Listing not found: " + listingId);
	}

	public ListingNotFoundException(String message) {
		super(message);
	}
}
