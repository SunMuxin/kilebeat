package com.neusoft.aclome.kilebeat.guice;

import java.io.File;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.neusoft.aclome.kilebeat.akka.ConfigWatcherActor;
import com.neusoft.aclome.kilebeat.configuration.ConfigurationValidator;
import com.neusoft.aclome.kilebeat.configuration.ConfigurationValidator.ExportsConfiguration;
import com.neusoft.aclome.kilebeat.configuration.ConfigurationValidator.ValidationResponse;
import com.neusoft.aclome.kilebeat.service.FileSystemWatcherService;

public class KileModule implements Module {
	
	public final String config;
	
	public KileModule(String config){
		this.config = config;
	}
	
	@Override
	public void configure(Binder binder) {
		
		final ValidationResponse validResp = new ConfigurationValidator().isValidScans(new File(config));		
		
		if (!validResp.isValid()) {
			System.err.println("config.file is INVALID ... exit!!?");
			System.exit(-1);
		}
		
		binder
			.bind(ConfigWatcherActor.class)
			.in(Singleton.class);

		binder
			.bind(FileSystemWatcherService.class)
			.in(Singleton.class);

		binder
			.bind(ExportsConfiguration.class)
			.toInstance(validResp.getConfig());
		
	}
}
