package com.almoxarifado.shared.infrastructure.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "almoxarifado.cors")
public record CorsProperties(@DefaultValue("http://localhost:4200") List<String> allowedOrigins) {
}
