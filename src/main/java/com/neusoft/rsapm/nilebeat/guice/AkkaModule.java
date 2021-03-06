package com.neusoft.rsapm.nilebeat.guice;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Singleton;
import com.neusoft.rsapm.nilebeat.akka.ConfigWatcherActor;
import com.neusoft.rsapm.nilebeat.configuration.EndpointsConfigurationValidator;
import com.neusoft.rsapm.nilebeat.configuration.ExportsConfigurationValidator;
import com.neusoft.rsapm.nilebeat.configuration.EndpointsConfigurationValidator.EndpointsConfiguration;
import com.neusoft.rsapm.nilebeat.configuration.EndpointsConfigurationValidator.EndpointsValidationResponse;
import com.neusoft.rsapm.nilebeat.configuration.ExportsConfigurationValidator.ExportsConfiguration;
import com.neusoft.rsapm.nilebeat.configuration.ExportsConfigurationValidator.ExportsValidationResponse;
import com.neusoft.rsapm.nilebeat.service.FileSystemWatcherService;
import com.typesafe.config.ConfigFactory;

public class AkkaModule implements Module {	
	
	@Override
	public void configure(Binder binder) {
		
		final ExportsValidationResponse exportsValidResp = 
				new ExportsConfigurationValidator().isValidExports(ConfigFactory.load("nilebeat"));		
		final EndpointsValidationResponse endpointValidResp = 
				new EndpointsConfigurationValidator().isValidEndpoint(ConfigFactory.load("nilebeat"));
			
		if (!exportsValidResp.isValid() || !endpointValidResp.isValid()) {
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
			.toInstance(exportsValidResp.getConfig());
		
		binder
			.bind(EndpointsConfiguration.class)
			.toInstance(endpointValidResp.getConfig());
		
	}
}
