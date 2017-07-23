package com.skillbill.at.akka;

import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skillbill.at.guice.GuiceAbstractActor;

import akka.actor.ActorIdentity;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.Identify;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetrieveActors extends GuiceAbstractActor {
	private final static Logger LOG = LoggerFactory.getLogger("internal");
	private final String identifyId = RandomStringUtils.randomNumeric(42);	
	
    @Override
    public void preStart() throws Exception {
    	super.preStart();
    	LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent());
    	        
    	getContext().actorSelection("/*").tell(new Identify(identifyId), getSelf());
    }

	@Override
	public void postStop() throws Exception {
		super.postStop();		
		LOGGER.info("end {} ", getSelf().path());
	}
        
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(ActorIdentity.class, identity -> {
				
				if (identity.correlationId().equals(identifyId)) {
					final Optional<ActorRef> ref = identity.getActorRef();
					
					if (ref.isPresent()) {
						final ActorPath path = ref.get().path();
						
						// Log or store the identity of the actor who replied
						LOG.info("The actor " + path.toString() + " exists and has replied!");
						
						// We want to discover all children of the received actor (recursive traversal)
						getContext().actorSelection(path.toString() + "/*").tell(new Identify(identifyId), getSelf());
					}				
				}				
				
			})
			.build();
	}

}
