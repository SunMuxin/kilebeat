package com.neusoft.aclome.nilebeat.akka;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.neusoft.aclome.nilebeat.guice.GuiceAbstractActor;
import com.neusoft.aclome.nilebeat.util.file.FileMonitorService;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.FiniteDuration;

@Slf4j
public class ConfigWatcherActor extends GuiceAbstractActor {	
	private static final String SCHEDULATION_WATCH = "SchedulationsWatch";
		
	private final FileMonitorService service;
	private final ActorSystem system = getContext().system();
	private Cancellable schedule;
	
	@Inject
	public ConfigWatcherActor() throws IOException {
		this.service = new FileMonitorService(new File(System.getProperty("config.file")));		
		
		this.schedule = system.scheduler().scheduleOnce(FiniteDuration.create(10, TimeUnit.SECONDS), 
			getSelf(), SCHEDULATION_WATCH, system.dispatcher(), getSelf());
		
		this.service.resolveActualFiles().forEach(file -> buildKileBeatActorFor(file.getAbsolutePath()));
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
				service.resolveEvents().forEach(file -> buildKileBeatActorFor(file.getAbsolutePath()));

				this.schedule = system.scheduler().scheduleOnce(FiniteDuration.create(10, TimeUnit.SECONDS), 
					getSelf(), SCHEDULATION_WATCH, system.dispatcher(), getSelf());
				
			})
			.matchAny(o -> {
				LOGGER.warn("not handled message", o);
				unhandled(o);
			})
			.build();
	}
	
	private void buildKileBeatActorFor(String path) {
		LOGGER.info("### build new KileBeatActor for " + path);				

		system
			.actorSelection("/user/kile-beat")
			.tell(path, ActorRef.noSender());
	}
			
}