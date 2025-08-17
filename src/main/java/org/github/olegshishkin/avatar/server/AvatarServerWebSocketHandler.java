package org.github.olegshishkin.avatar.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.github.olegshishkin.avatar.server.eventbus.EventBus;
import org.github.olegshishkin.avatar.server.eventbus.ReactiveEventHandler;
import org.github.olegshishkin.avatar.server.events.ReceiveMessageEvent;
import org.github.olegshishkin.avatar.server.events.SendMessageEvent;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Slf4j
@RequiredArgsConstructor
@Component
public class AvatarServerWebSocketHandler implements WebSocketHandler {

    private static final Map<String, Sinks.Many<Payload>> OUTBOUND_SINKS = new ConcurrentHashMap<>();

    private final ObjectMapper mapper;
    private final EventBus eventBus;

    @Override
    public @NonNull Mono<Void> handle(@NonNull WebSocketSession session) {
        log.info("WebSocket-обработчик запущен");
        var outboundSink = Sinks.many().unicast().<Payload>onBackpressureBuffer();
        OUTBOUND_SINKS.put(session.getId(), outboundSink);
        return Mono.zip(getReceiver(session), getSender(session)).then();
    }

    @ReactiveEventHandler(SendMessageEvent.class)
    public void onSendMessageEvent(SendMessageEvent event) {
        var payload = event.getPayload();
        log.trace("[{}] Событие отправки сообщения: {}", payload.sessionId(), payload);
        var outboundSink = OUTBOUND_SINKS.get(event.getWebSocketSessionId());
        if (outboundSink == null) {
            throw new NoSuchElementException("Не найдена сессия");
        }
        outboundSink.tryEmitNext(event.getPayload());
    }

    private Mono<Void> getReceiver(WebSocketSession session) {
        return session.receive()
                .map(m -> deserialize(m.getPayloadAsText()))
                .doOnNext(payload -> {
                    log.info("[{}] Получено: {}", payload.sessionId(), payload.message());
                    eventBus.publish(new ReceiveMessageEvent(session.getId(), payload));
                })
                .doOnError(e -> log.error("Ошибка при приеме сообщения", e))
                .doOnTerminate(() -> {
                    if (!session.isOpen()) {
                        removeWebSocketSession(session);
                    }
                })
                .then();
    }

    private Mono<Void> getSender(WebSocketSession session) {
        var outboundSink = OUTBOUND_SINKS.get(session.getId());
        if (outboundSink == null) {
            throw new NoSuchElementException("Не найдена сессия");
        }
        var messages = outboundSink
                .asFlux()
                .map(p -> {
                    log.info("[{}] Отправка: {}", p.sessionId(), p.message());
                    return session.textMessage(serialize(p));
                });
        return session.send(messages)
                .doOnError(e -> log.error("Ошибка отправки сообщения", e))
                .doOnTerminate(() -> {
                    if (!session.isOpen()) {
                        removeWebSocketSession(session);
                    }
                });
    }

    @SneakyThrows
    private String serialize(Payload payload) {
        return mapper.writeValueAsString(payload);
    }

    @SneakyThrows
    private Payload deserialize(String payload) {
        return mapper.readValue(payload, Payload.class);
    }

    private void removeWebSocketSession(WebSocketSession session) {
        log.warn("Удаляем данные WebSocket-сессии {}", session.getId());
        OUTBOUND_SINKS.remove(session.getId());
    }
}
