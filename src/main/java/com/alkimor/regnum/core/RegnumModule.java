package com.alkimor.regnum.core;

import net.neoforged.bus.api.IEventBus;

/**
 * Подмодуль Regnum. Контент модуля объявляется статическими полями в его классе
 * (через общие реестры {@link ModRegistries}), а в {@link #init(IEventBus)} модуль
 * подписывает свои обработчики на шины событий.
 */
public interface RegnumModule {
    /** Технический id модуля (латиница), используется в конфиге. */
    String id();

    /** Название модуля для логов и интерфейса. */
    String title();

    void init(IEventBus modBus);
}
