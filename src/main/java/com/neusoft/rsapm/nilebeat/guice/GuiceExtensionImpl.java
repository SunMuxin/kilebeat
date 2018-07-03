package com.neusoft.rsapm.nilebeat.guice;

import akka.actor.Extension;
import com.google.inject.Injector;

public class GuiceExtensionImpl implements Extension {
    private volatile Injector injector;

    public Injector getInjector() {
        return injector;
    }

    public void setInjector(Injector injector) {
        this.injector = injector;
    }
}
