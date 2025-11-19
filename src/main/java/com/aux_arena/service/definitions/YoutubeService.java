package com.aux_arena.service.definitions;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

@Service
public interface YoutubeService {

    JsonNode searchMusic(String query);

}
