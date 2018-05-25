package com.neusoft.aclome.kilebeat.configuration;

import com.typesafe.config.Config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**  
	* @ClassName: SolrEndPointConfiuration  
	* @Description: 
	* @author Muxin Sun
	* @date 2018年5月22日  
	*    
*/

@Setter
@Getter
@ToString
@EqualsAndHashCode
public class SolrEndPointConfiuration implements ConfigurationEndpoint {
	
	private String url;
	private String core;
	private String username;
	private String password;
	
	public SolrEndPointConfiuration(Config config) {
		this.url = config.getString("url");
		this.core = config.getString("core");
		this.username = config.hasPath("username")?config.getString("username"):null;
		this.password = config.hasPath("password")?config.getString("password"):null;
	}
}
