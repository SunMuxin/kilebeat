package com.neusoft.rsapm.nilebeat.akka.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.neusoft.rsapm.nilebeat.configuration.EndpointConfiguration;

import lombok.ToString;

@ToString
public class EndPointFailed implements Serializable {
	/**  
	 * @Fields 失败原因
	 * @author Muxin Sun
	 * @date 2018年6月6日
	*/
	private static final long serialVersionUID = 1716793153486578811L;
	private final EndpointConfiguration conf;
	private final LocalDateTime now; 

	public EndPointFailed(EndpointConfiguration conf) {
		this.conf = conf;
		this.now = LocalDateTime.now();
	}

	public EndpointConfiguration getConf() {
		return conf;
	}

	public boolean isExpired() {
		return ChronoUnit.SECONDS.between(now, LocalDateTime.now()) > 60;
	}
}
