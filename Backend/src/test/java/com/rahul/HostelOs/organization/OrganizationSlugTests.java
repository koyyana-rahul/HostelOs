package com.rahul.HostelOs.organization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrganizationSlugTests {

	@Test
	void appliesNfkcCaseFoldingAndTrim() {
		assertThat(OrganizationSlug.normalize("  ＳＵＮＲＩＳＥ-ＨＯＳＴＥＬ  "))
				.isEqualTo("sunrise-hostel");
	}

	@Test
	void acceptsValidSlugs() {
		assertThat(OrganizationSlug.normalize("abc")).isEqualTo("abc");
		assertThat(OrganizationSlug.normalize("sunrise-hostel-1")).isEqualTo("sunrise-hostel-1");
		assertThat(OrganizationSlug.normalize("a".repeat(60))).isEqualTo("a".repeat(60));
	}

	@Test
	void rejectsNull() {
		assertThatThrownBy(() -> OrganizationSlug.normalize(null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("null");
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "  \t\n  "})
	void rejectsBlank(String blank) {
		assertThatThrownBy(() -> OrganizationSlug.normalize(blank))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 3 and 60");
	}

	@Test
	void rejectsTooShort() {
		assertThatThrownBy(() -> OrganizationSlug.normalize("ab"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 3 and 60");
	}

	@Test
	void rejectsTooLong() {
		assertThatThrownBy(() -> OrganizationSlug.normalize("a".repeat(61)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("between 3 and 60");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"-leading-hyphen",
			"trailing-hyphen-",
			"double--hyphen",
			"slug_with_underscore",
			"slug with space",
			"slug.with.dot",
			"slug@symbol"
	})
	void rejectsInvalidCharactersAndFormats(String invalid) {
		assertThatThrownBy(() -> OrganizationSlug.normalize(invalid))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("lowercase alphanumeric characters separated by single hyphens");
	}
}
