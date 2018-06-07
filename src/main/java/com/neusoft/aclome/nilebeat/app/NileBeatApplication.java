package com.neusoft.aclome.nilebeat.app;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.neusoft.aclome.nilebeat.akka.BulkTimeoutActor;
import com.neusoft.aclome.nilebeat.akka.FileSystemWatcherActor;
import com.neusoft.aclome.nilebeat.akka.ManagerActor;
import com.neusoft.aclome.nilebeat.akka.RetrieveActors;
import com.neusoft.aclome.nilebeat.guice.GuiceActorUtils;
import com.neusoft.aclome.nilebeat.guice.GuiceExtension;
import com.neusoft.aclome.nilebeat.guice.GuiceExtensionImpl;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NileBeatApplication {
	
	private final Injector injector;
    private final ActorSystem system;        
    
	@Inject
	public NileBeatApplication(Injector injector) {
		this.system = ActorSystem.create("NilebeatSystem", ConfigFactory.load("nilebeat"));
		this.injector = injector;
	}
	
	public void run() throws Exception {

		//Add shutdownhook
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
	        LOGGER.info("-------------------------------------------------");
	        LOGGER.info(" NileBeat STOPPED");
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
        	GuiceActorUtils.makeProps(system, ManagerActor.class), "manager"
    	);
        
        system.actorOf(
        	GuiceActorUtils.makeProps(system, FileSystemWatcherActor.class), "watcher"
    	);

        system.actorOf(
        	GuiceActorUtils.makeProps(system, BulkTimeoutActor.class), "bulk-timeout"
    	);
        
        LOGGER.info("-------------------------------------------------");
        LOGGER.info(" NileBeat STARTED");
        LOGGER.info("-------------------------------------------------");

    }
	
	public void stop() {
        system.terminate();
	}
}
