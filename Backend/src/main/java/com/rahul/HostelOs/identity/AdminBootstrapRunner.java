//package com.rahul.HostelOs.identity;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.ApplicationArguments;
//import org.springframework.boot.ApplicationRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//public class AdminBootstrapRunner implements ApplicationRunner {
//
//	private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
//	private static final int MINIMUM_PASSWORD_LENGTH = 16;
//
//	private final AdminBootstrapProperties properties;
//	private final UserAccountService userAccounts;
//
//	public AdminBootstrapRunner(AdminBootstrapProperties properties, UserAccountService userAccounts) {
//		this.properties = properties;
//		this.userAccounts = userAccounts;
//	}
//
//	@Override
//	public void run(ApplicationArguments arguments) {
//		if (!properties.enabled()) {
//			return;
//		}
//		if (isBlank(properties.loginIdentifier())
//				|| isBlank(properties.displayName())
//				|| isBlank(properties.password())) {
//			throw new IllegalStateException("Bootstrap administrator configuration is incomplete");
//		}
//		if (properties.password().length() < MINIMUM_PASSWORD_LENGTH) {
//			throw new IllegalStateException("Bootstrap administrator password must be at least 16 characters");
//		}
//		boolean created = userAccounts.createPlatformAdminIfAbsent(
//				properties.loginIdentifier(),
//				properties.displayName(),
//				properties.password());
//		if (created) {
//			log.info("Bootstrap administrator account created");
//		} else {
//			log.info("Bootstrap administrator account already exists; no changes made");
//		}
//	}
//
//	private boolean isBlank(String value) {
//		return value == null || value.isBlank();
//	}
//}





package com.rahul.HostelOs.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    private static final int MINIMUM_PASSWORD_LENGTH = 16;

    private final AdminBootstrapProperties properties;
    private final UserAccountService userAccounts;

    public AdminBootstrapRunner(
            AdminBootstrapProperties properties,
            UserAccountService userAccounts) {
        this.properties = properties;
        this.userAccounts = userAccounts;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!properties.enabled()) {
            return;
        }

        if (isBlank(properties.loginIdentifier())
                || isBlank(properties.displayName())
                || isBlank(properties.password())) {
            throw new IllegalStateException(
                    "Bootstrap administrator configuration is incomplete");
        }

        if (properties.password().length() < MINIMUM_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "Bootstrap administrator password must be at least 16 characters");
        }

        boolean created = userAccounts.createPlatformAdminIfAbsent(
                properties.loginIdentifier(),
                properties.displayName(),
                properties.password());

        if (created) {
            log.info("Bootstrap administrator account created");
        } else {
            log.info("Bootstrap administrator account already exists; no changes made");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
