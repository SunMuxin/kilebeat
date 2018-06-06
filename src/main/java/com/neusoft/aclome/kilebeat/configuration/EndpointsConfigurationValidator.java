package com.neusoft.aclome.kilebeat.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

import com.neusoft.aclome.kilebeat.service.Endpoint;
import com.typesafe.config.Config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class EndpointsConfigurationValidator {
	
	public EndpointsValidationResponse isValidEndpoint(Config load) {
		if (load.isEmpty()) {
			throw new IllegalArgumentException("configuration file not valid");
		}
		
		final EndpointsValidationResponse response = new EndpointsValidationResponse();
		final Config eConfig = load;
		
		boolean hasHttp = eConfig.hasPath("http");
		boolean hasKafka = eConfig.hasPath("kafka");
		boolean hasSolr = eConfig.hasPath("solr");
		
		if (hasHttp) {
			if (!eConfig.hasPath("http.url")) {
				response.addError(String.format("http element does not contains %s", "http.url"));
			}
		}
		
		if (hasKafka) {
			if (!eConfig.hasPath("kafka.queue")) {
				response.addError(String.format("kafka element does not contains %s", "kafka.queue"));
			}
		}
		
		if (hasSolr) {
			if (!eConfig.hasPath("solr.core")) {
				response.addError(String.format("solr element does not contains %s", "solr.core"));
			}
			if (!eConfig.hasPath("solr.url")) {
				response.addError(String.format("solr element does not contains %s", "solr.url"));
			}
		}
		
		boolean hasBulk = eConfig.hasPath("bulk");
		
		if (hasBulk) {
			if (!eConfig.hasPath("bulk.size")) {
				response.addError(String.format("bulk element does not contain mandatory 'bulk.size' property"));
			}
		}
		
		final Bulk bulk = new Bulk(
				eConfig.hasPath("bulk.size") ? eConfig.getInt("bulk.size") : null,
				eConfig.hasPath("bulk.timeout") ? eConfig.getInt("bulk.timeout") : null
			);
		
		response.setBulk(bulk);
		
		if (response.isValid()) {
			Arrays.stream(Endpoint.values()).forEach(e -> {		

				final Config config = eConfig.hasPath(e.getConfKey()) ? eConfig.getObject(e.getConfKey()).toConfig() : null;

				//XXX potrebbe diventare if (config != null) {
				//visto che la validazione è fatta a priori!!!
				
				if (config != null && !config.isEmpty()) {
					response.addEndpoint(e.buildEndpoint(config));
				}
			});
		}
		
		
		
		return response;
	}
	

	public final class EndpointsValidationResponse {	
		private final List<EndpointConfiguration> configs;
		private final List<String> errors;
		@Setter
		private Bulk bulk;
		private EndpointsValidationResponse() {
			this.configs = new ArrayList<>();
			this.errors = new ArrayList<>();
			this.bulk = new Bulk(null, null);
		}

		public boolean isValid() {
			return errors.isEmpty();
		}	
		
		private void addError(String e) {
			errors.add(e);
		}
		
		private void addEndpoint(EndpointConfiguration endpoint) {
			configs.add(endpoint);
		}
		
		public EndpointsConfiguration getConfig() {
			return new EndpointsConfiguration(
				configs, 
				bulk
			);
		}
	}	
	
	public final class EndpointsConfiguration {
		private final SortedMap<Integer, EndpointConfiguration> endpoints;
		@Getter
		private final Bulk bulk;
		
		private EndpointsConfiguration(List<EndpointConfiguration> e, Bulk bulk) {
			this.bulk = bulk;
			this.endpoints = new TreeMap<>();
			IntStream.range(0, e.size()).forEachOrdered(i -> {
				this.endpoints.put(i, e.get(i));
			});
		}	
		
		//return sorted by key !!?
		public Collection<EndpointConfiguration> getEndpoints() { 			
			return endpoints.values();
		}
	}	
	

	// -----------------------------------------------------------------------
	
	@ToString
	public final class Bulk {		
		@Getter
		private final Integer size;
		
		@Getter
		private final Integer timeout;
		
		public Bulk(Integer size, Integer timeout) {
			this.size = size;
			this.timeout = timeout;
		}
		
		public boolean isAvailable() {
			return size != null; //timeout is OPTIONAL
		}		
	}
	
}
