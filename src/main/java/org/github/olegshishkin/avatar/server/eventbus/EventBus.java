package org.github.olegshishkin.avatar.server.eventbus;

import reactor.core.publisher.Flux;

public interface EventBus {

    <T extends Event> void publish(T event);

    Flux<Event> events();
}
