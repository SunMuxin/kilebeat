package com.neusoft.aclome.kilebeat.akka;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.neusoft.aclome.kilebeat.app.KileBeatApplication;
import com.neusoft.aclome.kilebeat.guice.AkkaModule;
import com.neusoft.aclome.kilebeat.guice.GuiceAbstractActor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KileManagerActor extends GuiceAbstractActor {	
		
	private final Map<String, KileBeatApplication> kilebeats;
	
	@Inject
	public KileManagerActor() {
		this.kilebeats = new HashMap<String, KileBeatApplication>();
	}
	
	@Override
	public void postStop() throws Exception {
		super.postStop();		
		LOGGER.info("end {} ", getSelf().path());
		
		kilebeats.entrySet().forEach(e -> {
			e.getValue().stop();
		});
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();		
		LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent().path());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(String.class, p -> {
				File f = new File(p);
				
				if (kilebeats.containsKey(p)) {
					kilebeats.get(p).stop();
				}
				if (f.exists()) {
					kilebeats.put(p, Guice
							.createInjector(new AkkaModule(p))
							.getInstance(KileBeatApplication.class));
					kilebeats.get(p).run();
				} else {
					kilebeats.remove(p);
				}
				//XXX this actor should be watched ? 
				//getContext().watch(tailActor);
			})
			.matchAny(o -> {
				LOGGER.warn("not handled message", o);
				unhandled(o);
			})
			.build();							
	}

}
