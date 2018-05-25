package com.neusoft.aclome.kilebeat.configuration;

import com.typesafe.config.Config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@EqualsAndHashCode
public class HttpEndPointConfiuration implements ConfigurationEndpoint {
	private String path;
	
	public HttpEndPointConfiuration(Config config) {
		this.path = config.getString("url");
	}
}
