package org.github.olegshishkin.avatar.server;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.github.olegshishkin.avatar.server.eventbus.EventBus;
import org.github.olegshishkin.avatar.server.eventbus.ReactiveEventHandler;
import org.github.olegshishkin.avatar.server.events.ReceiveMessageEvent;
import org.github.olegshishkin.avatar.server.events.SendMessageEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Service
public class AvatarServerService {

    private final Supplier<Flux<String>> MOCKED_RESPONSES = () -> Flux.range(1, 3)
            .concatMap(i -> {
                var element = String.valueOf(i);
                var millis = ThreadLocalRandom.current().nextInt(3000);
                return Mono.delay(Duration.ofMillis(millis))
                        .thenReturn(element);
            })
            .take(ThreadLocalRandom.current().nextInt(3));

    private final EventBus eventBus;

    @ReactiveEventHandler(ReceiveMessageEvent.class)
    public void onReceiveMessageEvent(ReceiveMessageEvent event) {
        var payload = event.getPayload();
        log.trace("[{}] Событие приема сообщения: {}", payload.sessionId(), payload);
        MOCKED_RESPONSES.get()
                .doOnNext(i -> {
                    var message = "%s-%s".formatted(payload.message(), i);
                    var response = new Payload(payload.sessionId(), message);
                    eventBus.publish(new SendMessageEvent(event.getWebSocketSessionId(), response));
                })
                .doOnComplete(() -> {
                    var sessionId = payload.sessionId();
                    log.debug("[{}] Обработано: {}", sessionId, payload.message());
                })
                .subscribe();
    }
}
