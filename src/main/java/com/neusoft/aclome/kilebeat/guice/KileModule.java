package com.neusoft.aclome.kilebeat.guice;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.neusoft.aclome.kilebeat.akka.ConfigWatcherActor;

public class KileModule implements Module {
	
	@Override
	public void configure(Binder binder) {
		
		binder
			.bind(ConfigWatcherActor.class)
			.in(Singleton.class);
		
	}
}
