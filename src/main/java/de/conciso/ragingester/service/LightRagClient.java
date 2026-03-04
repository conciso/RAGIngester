package de.conciso.ragingester.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.conciso.ragingester.config.LightRagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.file.Path;

/**
 * HTTP client for the LightRAG REST API.
 */
@Service
public class LightRagClient {

    private static final Logger log = LoggerFactory.getLogger(LightRagClient.class);

    private final RestClient restClient;

    public LightRagClient(LightRagProperties lightRagProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(lightRagProperties.url())
                .defaultHeader("X-API-Key", lightRagProperties.apiKey())
                .build();
    }

    /**
     * Uploads a single document via {@code POST /documents/upload} (multipart/form-data).
     *
     * @throws RestClientException on HTTP error
     */
    public void uploadDocument(Path file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));

        restClient.post()
                .uri("/documents/upload")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.debug("Uploaded: {}", file.getFileName());
    }

    /**
     * Returns the total number of indexed documents via {@code POST /documents/paginated}.
     */
    public int getTotalDocumentCount() {
        PaginatedResponse response = restClient.post()
                .uri("/documents/paginated")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PaginatedRequest(1, 100))
                .retrieve()
                .body(PaginatedResponse.class);

        return response != null ? response.total() : 0;
    }

    // ---- inner request / response records ----

    record PaginatedRequest(int page, @JsonProperty("page_size") int pageSize) {}

    record PaginatedResponse(int total) {}
}
