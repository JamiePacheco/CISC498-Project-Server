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

    @Value("{youtube.url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Logger log = LoggerFactory.getLogger(YoutubeServiceImpl.class);

    @Override
    public JsonNode searchMusic(String query) {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/search")
                .queryParam("part", "snippet")
                .queryParam("type", "video")
                .queryParam("videoCategoryId", "10")     // Music category
                .queryParam("videoEmbeddable", "true")   // Must be embeddable
                .queryParam("maxResults", "20")
                .queryParam("q", query)
                .queryParam("key", apiKey)
                .toUriString();

        return restTemplate.getForObject(url, JsonNode.class);
    }
}