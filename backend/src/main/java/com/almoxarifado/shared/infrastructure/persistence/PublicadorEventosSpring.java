package com.almoxarifado.shared.infrastructure.persistence;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.almoxarifado.shared.application.PublicadorEventos;
import com.almoxarifado.shared.events.IntegrationEvent;

@Component
public class PublicadorEventosSpring implements PublicadorEventos {
    private final ApplicationEventPublisher publisher;
    public PublicadorEventosSpring(ApplicationEventPublisher publisher) { this.publisher = publisher; }
    public void publicar(IntegrationEvent evento) { publisher.publishEvent(evento); }
}
