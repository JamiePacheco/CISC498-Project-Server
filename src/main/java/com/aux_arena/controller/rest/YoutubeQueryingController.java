package com.aux_arena.controller.rest;

import com.aux_arena.models.rest.Response;
import com.aux_arena.service.definitions.YoutubeService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/youtube")
public class YoutubeQueryingController {

    private YoutubeService youtubeService;

    public YoutubeQueryingController(YoutubeService youtubeService) {
        this.youtubeService = youtubeService;
    }


    @GetMapping()
    public ResponseEntity<Response<JsonNode>> search(@RequestParam("query") String query) {
        JsonNode results = youtubeService.searchMusic(query);
        return ResponseEntity.ok(
                Response.<JsonNode>builder()
                        .status(HttpStatus.ACCEPTED)
                        .responseContent(results)
                        .message("query ran")
                        .build()
            );
    }



}