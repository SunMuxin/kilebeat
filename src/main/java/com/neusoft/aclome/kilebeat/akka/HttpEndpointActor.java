package com.neusoft.aclome.kilebeat.akka;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import com.google.inject.Inject;
import com.neusoft.aclome.kilebeat.akka.dto.EndPointFailed;
import com.neusoft.aclome.kilebeat.akka.dto.NewLineEvent;
import com.neusoft.aclome.kilebeat.configuration.HttpEndPointConfiuration;
import com.neusoft.aclome.kilebeat.guice.GuiceAbstractActor;
import com.neusoft.aclome.kilebeat.retry.RetryCommand;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

import akka.actor.ActorRef;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpEndpointActor extends GuiceAbstractActor {
	
	private final Client client;
	private final HttpEndPointConfiuration conf;
	
	@Inject
	public HttpEndpointActor(HttpEndPointConfiuration conf) {
        final ClientConfig cc = new DefaultClientConfig();  
        cc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        cc.getSingletons().add(new JacksonJsonProvider());
        
        this.client = Client.create(cc);				        
        this.conf = conf;
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(NewLineEvent.class, s -> send(s))
			.matchAny(o -> {
				LOGGER.warn("not handled message", o);
				unhandled(o);
			})			
			.build();
	}
	
	@Override
	public void postStop() throws Exception {
		super.postStop();		
		LOGGER.info("end {} ", getSelf().path());
		
		client.destroy();		
		getContext().parent().tell(new EndPointFailed(conf), ActorRef.noSender());
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();		
		LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent().path());
	}

	private ClientResponse send(NewLineEvent s) {
		return new RetryCommand(3, s.getPath_s()).run(() -> {
			final WebResource resource = client.resource(conf.getPath());
			
			final ClientResponse response = resource
				.accept("application/json")
				.type("application/json")
				.post(ClientResponse.class, s);
			
			if (response.getStatus() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());			
			}
			
			return response;			
		});						
	}		
}	
