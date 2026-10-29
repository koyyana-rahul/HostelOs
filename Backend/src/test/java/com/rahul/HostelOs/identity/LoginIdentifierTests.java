package com.rahul.HostelOs.identity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginIdentifierTests {

	@Test
	void appliesNfkcCaseFoldingAndTrim() {
		assertThat(LoginIdentifier.normalize("  ＡＤＭＩＮ@ＥＸＡＭＰＬＥ.com  "))
				.isEqualTo("admin@example.com");
	}

	@Test
	void rejectsBlankIdentifiers() {
		assertThatThrownBy(() -> LoginIdentifier.normalize(" \t "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("blank");
	}
}
