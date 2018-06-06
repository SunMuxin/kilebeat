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
public class KafkaEndPointConfiuration implements EndpointConfiguration {
	private String queue;
	private String host;
	
	public KafkaEndPointConfiuration(Config config) {
		this.host = config.getString("host");
		this.queue = config.getString("queue");
	}
}
