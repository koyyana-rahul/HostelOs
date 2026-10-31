package com.rahul.HostelOs.inventory;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class PropertyCode {

	public static final int MIN_LENGTH = 2;
	public static final int MAX_LENGTH = 30;
	private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_-]{2,30}$");

	private PropertyCode() {
	}

	public static String normalize(String rawCode) {
		if (rawCode == null) {
			throw new IllegalArgumentException("Property code must not be null");
		}
		String normalized = Normalizer.normalize(rawCode, Normalizer.Form.NFKC)
				.trim()
				.toUpperCase(Locale.ROOT);
		validate(normalized);
		return normalized;
	}

	public static void validate(String normalizedCode) {
		if (normalizedCode == null || normalizedCode.length() < MIN_LENGTH || normalizedCode.length() > MAX_LENGTH) {
			throw new IllegalArgumentException(
					"Property code must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters");
		}
		if (!CODE_PATTERN.matcher(normalizedCode).matches()) {
			throw new IllegalArgumentException(
					"Property code must consist of uppercase alphanumeric characters, underscores, or hyphens");
		}
	}
}
