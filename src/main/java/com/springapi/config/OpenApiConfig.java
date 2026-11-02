package com.springapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Metadata shown at the top of Swagger UI ({@code /swagger-ui.html}).
 */
@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI apiInfo() {
		return new OpenAPI().info(new Info()
				.title("Spring API")
				.version("v1")
				.description("Library API: users, authors, books and bookmarks."));
	}

}
