package com.rahul.HostelOs.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
		title = "HostelOS API",
		version = "v1",
		description = "HostelOS backend API"))
@SecurityScheme(
		name = "sessionAuth",
		type = SecuritySchemeType.APIKEY,
		in = SecuritySchemeIn.COOKIE,
		paramName = "HOSTELOS_SESSION",
		description = "HttpOnly session cookie created by POST /api/v1/auth/login")
public class OpenApiConfig {
}
