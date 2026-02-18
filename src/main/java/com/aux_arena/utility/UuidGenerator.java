package com.aux_arena.utility;

import java.util.UUID;

public class UuidGenerator {
    public static String generateUuid() {
        UUID uuid = UUID.randomUUID();
        String uuidAsString = uuid.toString();
        return uuidAsString;
    }
}
