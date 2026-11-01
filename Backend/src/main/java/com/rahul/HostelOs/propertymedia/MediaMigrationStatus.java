package com.rahul.HostelOs.propertymedia;

public enum MediaMigrationStatus {
	NOT_STARTED,
	COPYING,
	VERIFIED,
	CUTOVER_READY,
	CUTOVER_COMPLETE,
	ROLLBACK_READY,
	FAILED
}
