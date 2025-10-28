package com.aux_arena.models.socket;

import com.aux_arena.models.enums.message.MessageStatus;
import com.aux_arena.models.enums.message.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Message<T> {

    private T messageContent;

    private MessageStatus messageStatus;

    private MessageType messageType;

    private String Message;

    private String errorMessage;
}
