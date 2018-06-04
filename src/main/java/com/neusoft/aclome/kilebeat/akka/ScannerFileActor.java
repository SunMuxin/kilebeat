package com.neusoft.aclome.kilebeat.akka;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.google.inject.Inject;
import com.neusoft.aclome.kilebeat.akka.dto.NewScanEvent;
import com.neusoft.aclome.kilebeat.configuration.ConfigurationEndpoint;
import com.neusoft.aclome.kilebeat.configuration.ConfigurationValidator.SingleConfiguration;
import com.neusoft.aclome.kilebeat.guice.GuiceAbstractActor;
import com.neusoft.aclome.kilebeat.service.Endpoint;
import com.neusoft.aclome.kilebeat.util.Util;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
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
public class ScannerFileActor extends GuiceAbstractActor {
	
	private final static String SCHEDULATION_CHECK = "SchedulationsCheck";

	private Cancellable schedule;
	private final SingleConfiguration conf;
	private final ActorSystem system;
	
	private Router router;

	@Inject
	public ScannerFileActor(SingleConfiguration conf) {
		
		this.conf = conf;
		
		if (conf.getBulk().isAvailable()) {
			this.router = new Router(new BulkBroadcastRoutingLogic(conf.getBulk().getSize()));
			
			registerToBulkTimeoutActor();
		} else {
			this.router = new Router(new BroadcastRoutingLogic());			
		}
		
		for (ConfigurationEndpoint ce : conf.getEndpoints()) {
			final Endpoint endpoint = Endpoint.valueOf(ce);
			
			router = router.addRoutee(buildRoutee(ce, endpoint));					
		}
		
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
				
				LOGGER.info("### Logger Scanner");
				
				NewScanEvent nse = new NewScanEvent(conf.getPath().getAbsolutePath(), new ArrayList<String>());
								
				Util.scanLogger(conf.getPath().toString()).forEach(f -> {
					if (conf.getRules().mustBeSent(f.getAbsolutePath())) {
						nse.addPath(f.getAbsolutePath());
					}
				});
				
				router.route(nse, ActorRef.noSender());
				
				this.schedule = system.scheduler().scheduleOnce(
						FiniteDuration.create(24, TimeUnit.HOURS), 
						getSelf(), SCHEDULATION_CHECK, 
						system.dispatcher(), getSelf());
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
	
	private Routee buildRoutee(ConfigurationEndpoint conf, Endpoint endpoint) {		
		final ActorRef child = getContext().actorOf(
			Props.create(endpoint.getActorClazz(), conf), endpoint.actorName()
		);
							
		getContext().watch(child); //to see Terminated event associated with 'child' actor
		
		return new ActorRefRoutee(child);		
	}
	
	private void registerToBulkTimeoutActor() {
		getContext()
			.actorSelection("/user/bulk-timeout")				
			.tell(conf.getBulk(), getSelf());
	}
}
