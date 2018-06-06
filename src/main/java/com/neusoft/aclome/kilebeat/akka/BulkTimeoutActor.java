package com.neusoft.aclome.kilebeat.akka;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.neusoft.aclome.kilebeat.configuration.EndpointsConfigurationValidator.Bulk;
import com.neusoft.aclome.kilebeat.guice.GuiceAbstractActor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.FiniteDuration;

@Slf4j
public class BulkTimeoutActor extends GuiceAbstractActor {
	
	public static final String BULK_TIMEOUT = "BULK_TIMEOUT";
	
	public Map<ActorRef, Cancellable> mapping = new HashMap<>();
		
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(Bulk.class, bConf -> {
				
				final ActorSystem system = getContext().system();
				final ActorRef sender = getSender();
				LOGGER.debug("received bulk config of {}", sender.path());
				
				if (mapping.containsKey(sender)) {
					mapping.remove(sender);
				} else {		
					final int bTimeout = bConf.getTimeout();
					
					if (bTimeout > 0) {
						final Cancellable cancel = system.scheduler().schedule(
							FiniteDuration.create(bTimeout, TimeUnit.SECONDS), 
							FiniteDuration.create(bTimeout, TimeUnit.SECONDS), 
							sender, BULK_TIMEOUT, system.dispatcher(), getSelf()
						);		
										
						mapping.put(sender, cancel);											
					}					
				}				
				
			})
			.build();
	}

	@Override
	public void postStop() throws Exception {
		super.postStop();
		LOGGER.info("end {} ", getSelf().path());
		
		mapping.values().forEach(c -> c.cancel());		
	}

	@Override
	public void preStart() throws Exception {
		super.preStart();
		LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent().path());
	}
	
}
