package de.conciso.ragingester.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Maps LIGHTRAG_URL and LIGHTRAG_API_KEY environment variables.
 * Spring relaxed binding: LIGHTRAG_URL → lightrag.url, LIGHTRAG_API_KEY → lightrag.api-key.
 */
@ConfigurationProperties(prefix = "lightrag")
public record LightRagProperties(String url, String apiKey) {
}
