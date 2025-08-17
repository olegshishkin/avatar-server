package org.github.olegshishkin.avatar.server.eventbus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
public class EventBusImpl implements EventBus {

    private static final Sinks.Many<Event> BUS = Sinks.many()
            .multicast()
            .onBackpressureBuffer();

    @Override
    public <T extends Event> void publish(T event) {
        log.trace("[EVENT BUS] Публикация события: {}", event);
        var result = BUS.tryEmitNext(event);
        if (result.isSuccess()) {
            log.trace("[EVENT BUS] Событие успешно опубликовано: {}", event);
        } else {
            log.error("[EVENT BUS] Ошибка при публикации события: {}", event);
        }
    }

    @Override
    public Flux<Event> events() {
        log.trace("[EVENT BUS] Подписка на чтение всех типов событий");
        return BUS.asFlux();
    }
}
