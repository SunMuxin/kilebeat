package com.neusoft.aclome.kilebeat.akka;

import static akka.actor.SupervisorStrategy.stop;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import com.neusoft.aclome.kilebeat.akka.dto.EndPointFailed;
import com.neusoft.aclome.kilebeat.akka.dto.NewLineEvent;
import com.neusoft.aclome.kilebeat.configuration.ConfigurationEndpoint;
import com.neusoft.aclome.kilebeat.configuration.ConfigurationValidator.SingleConfiguration;
import com.neusoft.aclome.kilebeat.guice.GuiceAbstractActor;
import com.neusoft.aclome.kilebeat.service.Endpoint;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.japi.pf.DeciderBuilder;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.Duration;

@Slf4j
public class TailerActor extends GuiceAbstractActor implements TailerListener {		
	
	private static final int DELAY = 50;
	private static final int BUFFER_SIZE = 1000;
	private static final boolean FROM_END = true;
	private static final boolean RE_OPEN = true;

	private final SingleConfiguration conf;	
	private final Tailer tailer;	
	private Router router;
		
	public TailerActor(SingleConfiguration conf) {
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

		this.tailer = Tailer.create(
			conf.getPath(), Charset.forName("UTF-8"), this, DELAY, FROM_END, RE_OPEN, BUFFER_SIZE
		);								
	}
		
	@Override
	public void postStop() throws Exception {
		super.postStop();
		LOGGER.info("end {} ", getSelf().path());
		
		tailer.stop();
		
		if (conf.getBulk().isAvailable()) {			
			registerToBulkTimeoutActor();
		}		
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent().path());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(NewLineEvent.class, s -> { //from self
				//LOGGER.info("[row] {}", s);
				
				if (conf.getRules().mustBeSent(s.getContent())) {
					router.route(s, ActorRef.noSender());	
				}
			})
			.match(Terminated.class, t -> {	//from any childs			
				final ActorRef fail = t.actor();
				LOGGER.warn("actor {} is terminated", fail.path());
				
				getContext().unwatch(fail);				
				router = router.removeRoutee(fail);																			
			})
			.match(EndPointFailed.class, f -> { //from any childs
				if (f.isExpired()) {
					LOGGER.info("expired {}", f);					
					final Endpoint endpoint = Endpoint.valueOf(f.getConf());
						
					router = router.addRoutee(buildRoutee(
						f.getConf(), endpoint
					));
				} else {
					LOGGER.info("NOT expired {}", f);
					getContext().parent().tell(f, getSelf());
				}
			})
			.matchEquals(BulkTimeoutActor.BULK_TIMEOUT, o -> { //from /user/bulk-timeout				
				if (conf.getBulk().isAvailable()) {
					router.route(o, ActorRef.noSender());
				}								
			})			
			.matchAny(o -> {
				LOGGER.warn("not handled message", o);
				unhandled(o);
			})
			.build();
	}

	@Override
	public void init(Tailer tailer) { } /* avoid interface segregation please !!? */

	@Override
	public void fileNotFound() { } /* avoid interface segregation please !!? */

	@Override
	public void fileRotated() { } /* avoid interface segregation please !!? */

	@Override
	public void handle(String line) {				
		getSelf().tell(
			new NewLineEvent(line, conf.getPath().toPath()), ActorRef.noSender()
		);
	}

	@Override
	public void handle(Exception ex) {
		LOGGER.error("[ => ]", ex);
		
		if (ex instanceof FileNotFoundException) { //occur when file is deleted during tailer are working on!!
			LOGGER.info("file to tail not found {}", conf.getPath());
			
			getSelf().tell(PoisonPill.getInstance(), ActorRef.noSender());
		}
	}
	
	@Override
	public SupervisorStrategy supervisorStrategy() {				
		//strategy is applied only to the child actor that failed
		return new OneForOneStrategy( 
			1, 
			Duration.create(1, TimeUnit.MINUTES), 
			DeciderBuilder.
				match(Exception.class, e -> stop())
//	        	match(ArithmeticException.class, e -> resume()).
//	        	match(NullPointerException.class, e -> restart()).
//	        	match(IllegalArgumentException.class, e -> stop()).
//	        	matchAny(o -> escalate())
				.build()
        );		
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
