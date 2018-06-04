package com.neusoft.aclome.kilebeat.app;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.neusoft.aclome.kilebeat.akka.BulkTimeoutActor;
import com.neusoft.aclome.kilebeat.akka.ExportsManagerActor;
import com.neusoft.aclome.kilebeat.akka.FileSystemWatcherActor;
import com.neusoft.aclome.kilebeat.akka.RetrieveActors;
import com.neusoft.aclome.kilebeat.guice.GuiceActorUtils;
import com.neusoft.aclome.kilebeat.guice.GuiceExtension;
import com.neusoft.aclome.kilebeat.guice.GuiceExtensionImpl;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KileBeatApplication {
	
	private final Injector injector;
    private final ActorSystem system;        
    
	@Inject
	public KileBeatApplication(Injector injector) {
		this.system = ActorSystem.create("kile", ConfigFactory.load("akka"));
		this.injector = injector;
	}
	
	public void run() throws Exception {

		//Add shutdownhook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
	        LOGGER.info("-------------------------------------------------");
	        LOGGER.info(" KileBeat STOPPED");
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
        	GuiceActorUtils.makeProps(system, ExportsManagerActor.class), "manager"
    	);     
        
        system.actorOf(
        	GuiceActorUtils.makeProps(system, FileSystemWatcherActor.class), "watcher"
    	);

        system.actorOf(
        	GuiceActorUtils.makeProps(system, BulkTimeoutActor.class), "bulk-timeout"
    	);
        
        LOGGER.info("-------------------------------------------------");
        LOGGER.info(" KileBeat STARTED");
        LOGGER.info("-------------------------------------------------");
        

    }
	
	public void stop() {
        system.terminate();
	}
}
