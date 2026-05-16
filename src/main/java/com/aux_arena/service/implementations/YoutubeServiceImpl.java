package com.aux_arena.service.implementations;

import com.aux_arena.service.definitions.YoutubeService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class YoutubeServiceImpl implements YoutubeService {

    // TODO change this to a env variable
    @Value("{youtube.api-key}")
    private String apiKey;

//    @Value("{youtube.url}")
    private String baseUrl = "https://www.googleapis.com/youtube/v3";

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Logger log = LoggerFactory.getLogger(YoutubeServiceImpl.class);

    @Override
    public JsonNode searchMusic(String query) {
        // 1. Appending -"#shorts" and -"shorts" to the query string
        // This is the most effective way to filter < 60s content when duration is 'any'
        String refinedQuery = query + " music -\"shorts\"";

        URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/search")
                .queryParam("part", "snippet")
                .queryParam("type", "video")
//                .queryParam("videoCategoryId", "10")     // Keep music category
                .queryParam("videoDuration", "any")      // Allows 2-4 minute songs
                .queryParam("videoEmbeddable", "true")
                .queryParam("maxResults", "20")
                .queryParam("q", refinedQuery)           // Using the refined query
                .queryParam("key", apiKey)
                .build()
                        .toUri();

        log.info("Requesting Music URL: {}", uri.getPath());

            // Reminder: This returns a JsonNode object, so no JSON.parse() is needed in your JS/TS

        JsonNode results = restTemplate.getForObject(uri, JsonNode.class);

        log.info("Fetched {} results from query '{}'", results.get("items").size(), query);

        return results;
    }
}