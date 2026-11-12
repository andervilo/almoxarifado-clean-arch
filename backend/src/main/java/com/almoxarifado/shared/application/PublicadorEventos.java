package com.almoxarifado.shared.application;

import com.almoxarifado.shared.events.IntegrationEvent;

public interface PublicadorEventos {
    void publicar(IntegrationEvent evento);
}
