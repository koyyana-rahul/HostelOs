package com.rahul.HostelOs.organization;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class OrganizationSlug {

	public static final int MIN_LENGTH = 3;
	public static final int MAX_LENGTH = 60;
	private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

	private OrganizationSlug() {
	}

	public static String normalize(String rawSlug) {
		if (rawSlug == null) {
			throw new IllegalArgumentException("Organization slug must not be null");
		}
		String normalized = Normalizer.normalize(rawSlug, Normalizer.Form.NFKC)
				.trim()
				.toLowerCase(Locale.ROOT);
		validate(normalized);
		return normalized;
	}

	public static void validate(String normalizedSlug) {
		if (normalizedSlug == null || normalizedSlug.length() < MIN_LENGTH || normalizedSlug.length() > MAX_LENGTH) {
			throw new IllegalArgumentException(
					"Organization slug must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters");
		}
		if (!SLUG_PATTERN.matcher(normalizedSlug).matches()) {
			throw new IllegalArgumentException(
					"Organization slug must consist of lowercase alphanumeric characters separated by single hyphens");
		}
	}
}
