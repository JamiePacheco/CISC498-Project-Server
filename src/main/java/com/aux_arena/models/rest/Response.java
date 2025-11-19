package com.aux_arena.models.rest;

import jdk.jfr.Name;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@Builder
public class Response<T> {
    private HttpStatus status;
    private String message;
    private T responseContent;
}
