package com.rahul.HostelOs.building;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validated, normalized building code.
 * <p>
 * Rules:
 * <ul>
 *   <li>2–30 characters after normalization and upper-casing.</li>
 *   <li>Only ASCII letters A–Z, digits 0–9, underscores and hyphens.</li>
 *   <li>Unicode is NFKC-normalized and folded to upper-case before validation.</li>
 * </ul>
 */
public final class BuildingCode {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 30;

    private static final Pattern VALID = Pattern.compile("^[A-Z0-9_-]{" + MIN_LENGTH + "," + MAX_LENGTH + "}$");

    private BuildingCode() {
    }

    /**
     * Normalize {@code raw}: NFKC → strip leading/trailing whitespace → upper-case.
     *
     * @param raw raw input; must not be {@code null}
     * @return normalized string ready for validation or persistence
     */
    public static String normalize(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Building code must not be null");
        }
        String nfkc = Normalizer.normalize(raw, Normalizer.Form.NFKC);
        return nfkc.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Validate a normalized building code.
     *
     * @param normalized result of {@link #normalize(String)}
     * @throws IllegalArgumentException if the code does not satisfy the rules
     */
    public static void validate(String normalized) {
        if (normalized == null || normalized.isEmpty()) {
            throw new IllegalArgumentException("Building code must not be blank");
        }
        if (!VALID.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Building code '" + normalized + "' is invalid. "
                    + "Use 2–30 characters: A–Z, 0–9, underscore or hyphen.");
        }
    }

    /**
     * Convenience method: normalize then validate.
     *
     * @param raw raw input
     * @return normalized code
     * @throws IllegalArgumentException if null, blank or fails pattern
     */
    public static String normalizeAndValidate(String raw) {
        String normalized = normalize(raw);
        validate(normalized);
        return normalized;
    }
}
