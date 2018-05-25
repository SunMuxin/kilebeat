package com.neusoft.aclome.kilebeat.guice;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import akka.actor.ExtensionId;
import akka.actor.ExtensionIdProvider;

public class GuiceExtension extends AbstractExtensionId<GuiceExtensionImpl> implements ExtensionIdProvider {

    public static final GuiceExtension provider = new GuiceExtension();

    @Override
    public GuiceExtensionImpl createExtension(ExtendedActorSystem system) {
        return new GuiceExtensionImpl();
    }

    @Override
    public ExtensionId<? extends Extension> lookup() {
        return provider;
    }
}