package org.github.olegshishkin.avatar.server.eventbus;

import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ReactiveEventHandlersRegistrar {

    private final ApplicationContext context;
    private final EventBus eventBus;

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent ignore) {
        log.trace("Регистрация реактивных обработчиков событий");
        context.getBeansWithAnnotation(Component.class)
                .values()
                .forEach(bean -> {
                    for (var method : bean.getClass().getDeclaredMethods()) {
                        var annotationType = ReactiveEventHandler.class;
                        var annotation = AnnotationUtils.findAnnotation(method, annotationType);
                        if (annotation != null) {
                            registerHandler(bean, method, annotation.value());
                        }
                    }
                });
    }

    private void registerHandler(Object bean, Method method, Class<? extends Event> eventType) {
        var methodName = method.getName();
        var eventName = eventType.getSimpleName();
        var className = bean.getClass().getSimpleName();
        eventBus.events()
                .ofType(eventType)
                .doOnNext(event -> {
                    try {
                        method.invoke(bean, event);
                    } catch (Exception e) {
                        throw new RuntimeException("Ошибка при вызове метода-обработчика", e);
                    }
                })
                .doOnSubscribe(subscription -> log.trace("Зарегистрирован обработчик {} из класса "
                        + "{} для события {}", methodName, className, eventName))
                .subscribe();
    }
}
