package com.neusoft.rsapm.nilebeat.akka;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.neusoft.rsapm.nilebeat.guice.GuiceAbstractActor;

import akka.actor.ActorIdentity;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.DeadLetter;
import akka.actor.Identify;
import akka.actor.Terminated;
import lombok.extern.slf4j.Slf4j;

/*
 * this actor is only for development purpose ... !!?
 *  
 * is used for identify all actors availabe in a particular time when a message arrived to the DeadLetter.
 */

@Slf4j
public class RetrieveActors extends GuiceAbstractActor {
	
	//use a different file for logging result of find
	private final static Logger LOG = LoggerFactory.getLogger("internal");
	
	private final String identifyId = UUID.randomUUID().toString();	
	
    @Override
    public void preStart() throws Exception {
    	super.preStart();
    	LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent().path());
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
				
				LOG.error("The actor " + identity.getActorRef() + " exists and has replied!");
				
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
			.match(DeadLetter.class, de -> {
				getContext().actorSelection("/*").tell(new Identify(identifyId), getSelf());
			})
			.match(Terminated.class, message -> {
				System.err.println("Terminated");
//				getContext().actorSelection("/").tell(message, getSelf());
		        getContext().stop(getSelf());
		    })
			.matchAny(o -> {
				LOGGER.warn("not handled message", o);
			})			
			.build();
	}

}
