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
import com.neusoft.aclome.kilebeat.akka.dto.Messages;
import com.neusoft.aclome.kilebeat.akka.dto.NewLineEvent;
import com.neusoft.aclome.kilebeat.configuration.EndpointConfiguration;
import com.neusoft.aclome.kilebeat.configuration.EndpointsConfigurationValidator.EndpointsConfiguration;
import com.neusoft.aclome.kilebeat.configuration.ExportsConfigurationValidator.SingleConfiguration;
import com.neusoft.aclome.kilebeat.guice.GuiceAbstractActor;
import com.neusoft.aclome.kilebeat.service.Endpoint;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.MemberUp;
import akka.routing.Router;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.FiniteDuration;

@Slf4j
public class ManagerActor extends GuiceAbstractActor {	
	private final static String SCHEDULATION_CHECK = "SchedulationsCheck";
		
	private final Map<ActorRef, List<EndPointFailed>> association;
	private final Cancellable schedule;
	private final Cluster cluster = Cluster.get(getContext().getSystem());
	private final EndpointsConfiguration conf;
	private Router router;

	@Inject
	public ManagerActor(EndpointsConfiguration conf) {
		this.conf = conf;
		this.association = new HashMap<>();
		
		final ActorSystem system = getContext().system();
		
		if (conf.getBulk().isAvailable()) {
			this.router = new Router(new BulkBroadcastRoutingLogic(conf.getBulk().getSize()));
			
			registerToBulkTimeoutActor();
		} else {
			this.router = new Router(new BroadcastRoutingLogic());			
		}
		
		conf.getEndpoints().forEach(ce -> {
			final Endpoint endpoint = Endpoint.valueOf(ce);
			router = router.addRoutee(buildRoutee(ce, endpoint));
		});
		
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
	    cluster.unsubscribe(getSelf());
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();		
		LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent().path());
		
		cluster.subscribe(getSelf(), MemberUp.class);
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
			.match(NewLineEvent.class, l -> {
				router.route(l, ActorRef.noSender());
			})
			.match(SingleConfiguration.class, sc -> {
				
				LOGGER.info("SingleConfiguration = {}", sc.getPath());
				
				getContext().actorOf(
					Props.create(TailerActor.class, sc), tailer()
				);

				//XXX this actor should be watched ? 
				//getContext().watch(tailActor);
			})
			.matchEquals(Messages.WISP_REGISTRATION, o -> { //from /user/bulk-timeout				
				getSender().tell(Messages.ANCIENT_REGISTRATION, getSelf());
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

	private List<EndPointFailed> getFailed(ActorRef sender) {
		if (!association.containsKey(sender)) {
			association.put(sender, new ArrayList<>());
		}
		
		return association.get(sender);
	}
	
	private Routee buildRoutee(EndpointConfiguration conf, Endpoint endpoint) {		
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
