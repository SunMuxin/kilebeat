package com.neusoft.rsapm.nilebeat.akka;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import akka.actor.ActorRef;
import akka.routing.Routee;
import lombok.extern.slf4j.Slf4j;
import scala.collection.JavaConverters;
import scala.collection.immutable.IndexedSeq;

@Slf4j
public class BulkBroadcastRoutingLogic implements akka.routing.RoutingLogic {
	
	//XXX same implementation of akka.routing.NoRoutee
	private final Routee DO_NOTHING = new Routee(){
		@Override
		public void send(Object message, ActorRef sender) {
		}
	};
	
	private final List<Object> messages = new ArrayList<>();	
	private final AtomicInteger counter;
	private final int upperBoundCounter;
	
	public BulkBroadcastRoutingLogic(int upperBoundCounter) {
		this.upperBoundCounter = upperBoundCounter;
		this.counter = new AtomicInteger(upperBoundCounter);
	}

	@Override
	public Routee select(Object message, IndexedSeq<Routee> routees) {
		LOGGER.debug("called with message {}", message);		
	    final List<Routee> items = JavaConverters.seqAsJavaListConverter(routees).asJava();
	    
		if (!items.isEmpty()) {			
			if (message instanceof String && StringUtils.equals(String.valueOf(message), BulkTimeoutActor.BULK_TIMEOUT)) {
				return buildBrocast(items);
			}
			
			messages.add(message);
			
			if (counter.get() == 1) {				
				return buildBrocast(items);
			} else {				
				counter.decrementAndGet();
			}	    										
	    }	    	    
	    
	    return DO_NOTHING;
	}

	private Routee buildBrocast(final List<Routee> items) {
		final Routee result = new BrodcastRoutee(
			messages.stream().map(m -> m).collect(Collectors.toList()), //XXX copy of messages!! 
			items
		);
				
		messages.clear();
		counter.getAndSet(upperBoundCounter);
		
		return result;
	}
	
	//XXX this implementation is useful ... it's a tricks
	class BrodcastRoutee implements Routee {		
		private final List<Object> messages;
		private final List<Routee> routee;

		public BrodcastRoutee(List<Object> messages, List<Routee> routee) {
			this.messages = messages;			
			this.routee = routee;
		}

		@Override
		public void send(Object message, ActorRef sender) {
			//XXX don't use message, because is only the last message and not the last n-th
			
			routee.forEach(r -> {				
				messages.forEach(m -> r.send(m, sender));				
			});
		}		
	}
	
}
