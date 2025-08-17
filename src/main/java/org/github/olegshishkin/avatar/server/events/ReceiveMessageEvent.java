package org.github.olegshishkin.avatar.server.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.github.olegshishkin.avatar.server.Payload;
import org.github.olegshishkin.avatar.server.eventbus.Event;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class ReceiveMessageEvent implements Event {

    private final String webSocketSessionId;
    private final Payload payload;
}
