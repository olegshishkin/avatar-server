package org.github.olegshishkin.avatar.server;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;

@Configuration
public class Config {

    @Bean
    public HandlerMapping handlerMapping(@Value("${ws-server.path}") String wsServerPath,
            AvatarServerWebSocketHandler handler) {
        var map = Map.of(wsServerPath, handler);
        int order = -1;
        return new SimpleUrlHandlerMapping(map, order);
    }
}
