package com.rahul.HostelOs.listing.exception;

public class DuplicateListingTitleException extends RuntimeException {

	public DuplicateListingTitleException(String title) {
		super("A listing with this title already exists: " + title);
	}
}
