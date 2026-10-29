package com.rahul.HostelOs.identity;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

public final class LoginIdentifier {

	private LoginIdentifier() {
	}

	public static String normalize(String rawIdentifier) {
		String normalized = Normalizer.normalize(Objects.requireNonNull(rawIdentifier), Normalizer.Form.NFKC)
				.trim()
				.toLowerCase(Locale.ROOT);
		if (normalized.isBlank()) {
			throw new IllegalArgumentException("Login identifier must not be blank");
		}
		return normalized;
	}
}
