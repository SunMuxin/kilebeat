package com.skillbill.at.integration;

import java.io.File;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import com.neusoft.aclome.kilebeat.akka.dto.NewLineEvent;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

public class PythonHttpServerIT {
	
	public static void main(String[] args) {

        final ClientConfig cc = new DefaultClientConfig();  
        cc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        cc.getSingletons().add(new JacksonJsonProvider());
        
        final Client client = Client.create(cc);	
        
        final WebResource resource = client.resource("http://localhost:8888");
        
        final NewLineEvent s = new NewLineEvent("CICCIO", new File("/tmp/").toPath());
		
		final ClientResponse response = resource
				.accept("application/json")
				.type("application/json")
				.post(ClientResponse.class, s);
		
		System.out.println(response.getStatus());
		
		System.out.println(response.getEntity(String.class));
		
	}

}
