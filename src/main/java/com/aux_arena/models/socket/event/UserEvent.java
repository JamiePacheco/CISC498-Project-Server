package com.aux_arena.models.socket.event;

import com.aux_arena.models.enums.message.MessageStatus;
import com.aux_arena.models.enums.message.UserEventType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserEvent<T> {

    private T messageContent;

    private MessageStatus messageStatus;

    private UserEventType userEventType;

    private String Message;

    private String errorMessage;

    private long sequence;
}
