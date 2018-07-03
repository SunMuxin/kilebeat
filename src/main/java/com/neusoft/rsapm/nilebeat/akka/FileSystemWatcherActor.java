package com.neusoft.rsapm.nilebeat.akka;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.neusoft.rsapm.nilebeat.configuration.ExportsConfigurationValidator.ExportsConfiguration;
import com.neusoft.rsapm.nilebeat.configuration.ExportsConfigurationValidator.SingleConfiguration;
import com.neusoft.rsapm.nilebeat.guice.GuiceAbstractActor;
import com.neusoft.rsapm.nilebeat.service.FileSystemWatcherService;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.FiniteDuration;

@Slf4j
public class FileSystemWatcherActor extends GuiceAbstractActor {	
	private static final String SCHEDULATION_WATCH = "SchedulationsWatch";
		
	private final FileSystemWatcherService service;
	
	private final ActorSystem system = getContext().system();
	private Cancellable schedule;
	
	@Inject
	public FileSystemWatcherActor(ExportsConfiguration config, FileSystemWatcherService service) throws IOException {
		this.service = service;
				
		this.schedule = system.scheduler().scheduleOnce(FiniteDuration.create(30, TimeUnit.SECONDS), 
			getSelf(), SCHEDULATION_WATCH, system.dispatcher(), getSelf());
		
		config.getExports().forEach(obj -> {			
			final File resource = obj.getPath();
			
			if (resource.exists()) {
				LOGGER.info("on path {} ... run tail", resource);				
				buildTailerActorFor(obj);								
			} else {
				LOGGER.info("on path {} ... can't run tail, but i watch it for new files that will be generated", resource);											
				service.resolveActualFiles(obj).forEach(sc -> buildTailerActorFor(sc));				
			}
		});						
	}
	
	@Override
	public void postStop() throws Exception {
		super.postStop();
		LOGGER.info("end {} ", getSelf().path());
		
		service.close();
		schedule.cancel();
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();		
		LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent().path());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.matchEquals(SCHEDULATION_WATCH, sw -> {				
				
				LOGGER.info("### check new files");				
				service.resolveEvents().forEach(sc -> buildTailerActorFor(sc));

				this.schedule = system.scheduler().scheduleOnce(FiniteDuration.create(30, TimeUnit.SECONDS), 
					getSelf(), SCHEDULATION_WATCH, system.dispatcher(), getSelf());
				
			})
			.matchAny(o -> {
				LOGGER.warn("not handled message", o);
				unhandled(o);
			})	
			.build();
	}
	
	private void buildTailerActorFor(SingleConfiguration sc) {
		system
			.actorSelection("/user/manager")
			.tell(sc, ActorRef.noSender());
	}
			
}