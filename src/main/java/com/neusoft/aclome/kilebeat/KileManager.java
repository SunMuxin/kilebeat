package com.neusoft.aclome.kilebeat;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.neusoft.aclome.kilebeat.akka.BulkTimeoutActor;
import com.neusoft.aclome.kilebeat.akka.ConfigWatcherActor;
import com.neusoft.aclome.kilebeat.akka.KileManagerActor;
import com.neusoft.aclome.kilebeat.akka.LoggerManagerActor;
import com.neusoft.aclome.kilebeat.akka.RetrieveActors;
import com.neusoft.aclome.kilebeat.guice.GuiceActorUtils;
import com.neusoft.aclome.kilebeat.guice.GuiceExtension;
import com.neusoft.aclome.kilebeat.guice.GuiceExtensionImpl;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KileManager {
	
	private final Injector injector;
    
	@Inject
	public KileManager(Injector injector) {
		this.injector = injector;
	}
	
	public void run() throws Exception {

		final ActorSystem system = ActorSystem.create("kile-manager", ConfigFactory.load());

		//Add shutdownhook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
	        LOGGER.info("-------------------------------------------------");
	        LOGGER.info(" KileManager STOPPED");
	        LOGGER.info("-------------------------------------------------");
			
            system.terminate();
        }));
		
		system.registerExtension(GuiceExtension.provider);

        //configure Guice
        final GuiceExtensionImpl guiceExtension = GuiceExtension.provider.get(system);
        guiceExtension.setInjector(injector);
        
        //XXX start only in development environment
        system
        	.eventStream()
        	.subscribe(
    	        system.actorOf(
	        		GuiceActorUtils.makeProps(system, RetrieveActors.class), "retrieve"
	    		), 
    			DeadLetter.class
			);

        //XXX create before watcher because ... manager use watcher internally   
        
        system.actorOf(
        	GuiceActorUtils.makeProps(system, KileManagerActor.class), "kile-beat"
    	);    
        
        system.actorOf(
        	GuiceActorUtils.makeProps(system, ConfigWatcherActor.class), "config"
    	);
        
        system.actorOf(
        	GuiceActorUtils.makeProps(system, LoggerManagerActor.class), "scanner-manager"
        );

        system.actorOf(
        	GuiceActorUtils.makeProps(system, BulkTimeoutActor.class), "bulk-timeout"
    	);
        
        LOGGER.info("-------------------------------------------------");
        LOGGER.info(" KileManager STARTED");
        LOGGER.info("-------------------------------------------------");
    }
}
