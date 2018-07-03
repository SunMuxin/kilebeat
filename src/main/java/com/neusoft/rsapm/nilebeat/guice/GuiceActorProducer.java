package com.neusoft.rsapm.nilebeat.guice;

import com.google.inject.Injector;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;

public class GuiceActorProducer implements IndirectActorProducer {
    private final Injector injector;
    private Class<? extends Actor> actorClass;

    public GuiceActorProducer(Injector injector, Class<? extends Actor> actorClass, Object ... arguments) {
        this.injector = injector;
        this.actorClass = actorClass;
    }

    @Override
    public Actor produce() {
    	return injector.getInstance(actorClass);
    }

    @Override
    public Class<? extends Actor> actorClass() {
        return actorClass;
    }
}