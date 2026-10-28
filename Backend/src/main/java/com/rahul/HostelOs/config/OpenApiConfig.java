package com.rahul.HostelOs.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
		title = "HostelOS API",
		version = "v1",
		description = "HostelOS backend API"))
public class OpenApiConfig {
}
