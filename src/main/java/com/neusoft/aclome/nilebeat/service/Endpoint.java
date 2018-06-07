package com.neusoft.aclome.nilebeat.service;

import java.util.UUID;

import com.neusoft.aclome.nilebeat.akka.GraphiteEndpointActor;
import com.neusoft.aclome.nilebeat.akka.HttpEndpointActor;
import com.neusoft.aclome.nilebeat.akka.KafkaEndpointActor;
import com.neusoft.aclome.nilebeat.akka.SolrEndpointActor;
import com.neusoft.aclome.nilebeat.configuration.EndpointConfiguration;
import com.neusoft.aclome.nilebeat.configuration.GraphiteEndPointConfiuration;
import com.neusoft.aclome.nilebeat.configuration.HttpEndPointConfiuration;
import com.neusoft.aclome.nilebeat.configuration.KafkaEndPointConfiuration;
import com.neusoft.aclome.nilebeat.configuration.SolrEndPointConfiuration;
import com.typesafe.config.Config;

import akka.actor.AbstractActor;
import lombok.Getter;

public enum Endpoint {

	HTTP("http", HttpEndPointConfiuration.class, HttpEndpointActor.class),
	KAFKA("kafka", KafkaEndPointConfiuration.class, KafkaEndpointActor.class),
	GRAPHITE("graphite", GraphiteEndPointConfiuration.class, GraphiteEndpointActor.class),
	
	/**  
    * @Fields push data to solr collection  
	* @author Muxin Sun
	* @date 2018-05-22
	*/  
	SOLR("solr", SolrEndPointConfiuration.class, SolrEndpointActor.class);

	@Getter
	private final String confKey;
	
	private final Class<? extends EndpointConfiguration> confClazz;
	
	@Getter
	private final Class<? extends AbstractActor> actorClazz; 

	private Endpoint(String confKey, Class<? extends EndpointConfiguration> confClazz, Class<? extends AbstractActor> actorClazz) {
		this.confKey = confKey;
		this.confClazz = confClazz;	
		this.actorClazz = actorClazz;
	}

	public EndpointConfiguration buildEndpoint(Config config) {		
		try {
			return confClazz.getConstructor(Config.class).newInstance(config);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}	
	}
	
	public String actorName() {
		return confKey + UUID.randomUUID().toString();
	}
	
	public static Endpoint valueOf(EndpointConfiguration ep) {
		try {			
			final Class<?> requested = Class.forName(ep.getClass().getName());
			
			for (Endpoint endpoint : values()) {
				if (requested.equals(endpoint.confClazz)) {
					return endpoint;
				}
			}					
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	
		throw new RuntimeException("unknow " + ep.getClass().getName());				
	}	
}
