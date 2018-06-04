package com.neusoft.aclome.kilebeat.akka;

import static com.neusoft.aclome.kilebeat.service.ActorNamesFactory.scanner;

import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.neusoft.aclome.kilebeat.configuration.ConfigurationValidator.ExportsConfiguration;
import com.neusoft.aclome.kilebeat.guice.GuiceAbstractActor;

import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.FiniteDuration;

/**  
    * @ClassName: ScanLoggerFileActor  
    * @Description: push GET LOOGER DIR
    * @author Muxin Sun
    * @date 2018-05-22  
    *    
*/  
@Slf4j
public class LoggerManagerActor extends GuiceAbstractActor {
	
	private final static String SCHEDULATION_CHECK = "SchedulationsCheck";

	private Cancellable schedule;
	private final ActorSystem system;
	private final ExportsConfiguration config;

	@Inject
	public LoggerManagerActor(ExportsConfiguration config) {
		
		this.config = config;
		
		this.system = getContext().system();
		
		this.schedule = system.scheduler().scheduleOnce(
				FiniteDuration.create(1, TimeUnit.SECONDS), 
				getSelf(), SCHEDULATION_CHECK, 
				system.dispatcher(), getSelf());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.matchEquals(SCHEDULATION_CHECK, sc -> {	
				
				LOGGER.info("### Logger Scan Manager");
				
				config.getExports().forEach(obj -> {
					
					LOGGER.info("on path {} ... scan logger file", obj.getPath());
					getContext().actorOf(
							Props.create(ScannerFileActor.class, obj), scanner()
						);
				});
				
			})
			.matchAny(o -> {
				LOGGER.warn("not handled message", o);
				unhandled(o);
			})
			.build();
	}
	
	@Override
	public void postStop() throws Exception {
		super.postStop();
		LOGGER.info("end {} ", getSelf().path());
		
		schedule.cancel();
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent().path());
	}
}	
