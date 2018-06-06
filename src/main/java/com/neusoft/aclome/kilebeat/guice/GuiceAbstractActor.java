package com.neusoft.aclome.kilebeat.guice;

import com.google.inject.Injector;

import akka.actor.AbstractActor;

public abstract class GuiceAbstractActor extends AbstractActor  {

	public Injector getInjector() {
        return GuiceExtension.provider.get(getContext().system()).getInjector();
    }
	
}
