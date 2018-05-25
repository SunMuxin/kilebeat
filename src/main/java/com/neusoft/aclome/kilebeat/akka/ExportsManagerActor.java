package com.neusoft.aclome.kilebeat.akka;

import static com.neusoft.aclome.kilebeat.service.ActorNamesFactory.tailer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.neusoft.aclome.kilebeat.akka.dto.EndPointFailed;
import com.neusoft.aclome.kilebeat.configuration.ConfigurationValidator.SingleConfiguration;
import com.neusoft.aclome.kilebeat.guice.GuiceAbstractActor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.FiniteDuration;

@Slf4j
public class ExportsManagerActor extends GuiceAbstractActor {	
	private final static String SCHEDULATION_CHECK = "SchedulationsCheck";
		
	private final Map<ActorRef, List<EndPointFailed>> association;
	private final Cancellable schedule;

	@Inject
	public ExportsManagerActor() {
		this.association = new HashMap<>();
		
		final ActorSystem system = getContext().system();
		
		schedule = system.scheduler().schedule(
			FiniteDuration.create(10, TimeUnit.SECONDS), 
			FiniteDuration.create(10, TimeUnit.SECONDS), 
			getSelf(), SCHEDULATION_CHECK, 
			system.dispatcher(), getSelf()
		);		
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
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(EndPointFailed.class, f -> {			
				getFailed(getSender()).add(f);
			})
			.matchEquals(SCHEDULATION_CHECK, sc -> {	
				final Set<ActorRef> actorRefs = association.keySet();											
				
				actorRefs.forEach(childActor -> {						
					final List<EndPointFailed> fails = association.get(childActor);
					LOGGER.info("### found {} failed conf for {}", fails.size(), childActor);
					
					for(int i = 0; i < fails.size(); i++) {						
						final EndPointFailed epf = fails.get(i);
						
						if (epf.isExpired()) {														
							childActor.tell(epf, ActorRef.noSender());
							fails.remove(i);
						}						
					}
				});
				
				//remove key's without values!!
				actorRefs.removeAll(
					actorRefs.stream()
						.filter(childActor -> association.get(childActor).isEmpty())
						.collect(Collectors.toList())
				);
			})
			.match(SingleConfiguration.class, sc -> {
				getContext().actorOf(
					Props.create(TailerActor.class, sc), tailer()
				);

				//XXX this actor should be watched ? 
				//getContext().watch(tailActor);
			})
			.matchAny(o -> {
				LOGGER.warn("not handled message", o);
				unhandled(o);
			})
			.build();							
	}

	private List<EndPointFailed> getFailed(ActorRef sender) {
		if (!association.containsKey(sender)) {
			association.put(sender, new ArrayList<>());
		}
		
		return association.get(sender);
	}
}
