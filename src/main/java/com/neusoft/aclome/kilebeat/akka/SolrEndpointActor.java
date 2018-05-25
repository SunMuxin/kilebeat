package com.neusoft.aclome.kilebeat.akka;

import java.util.concurrent.Callable;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.neusoft.aclome.kilebeat.akka.dto.EndPointFailed;
import com.neusoft.aclome.kilebeat.akka.dto.NewLineEvent;
import com.neusoft.aclome.kilebeat.configuration.SolrEndPointConfiuration;
import com.neusoft.aclome.kilebeat.guice.GuiceAbstractActor;
import com.neusoft.aclome.kilebeat.retry.RetryCommand;
import com.neusoft.aclome.kilebeat.util.solr.SolrWriter;

import akka.actor.ActorRef;
import lombok.extern.slf4j.Slf4j;

/**  
    * @ClassName: SolrEndpointActor  
    * @Description: push solr Actor 
    * @author Muxin Sun
    * @date 2018-05-22  
    *    
*/  
@Slf4j
public class SolrEndpointActor extends GuiceAbstractActor {
	
	private final SolrWriter sw;
	private final SolrEndPointConfiuration conf;
	
	@Inject
	public SolrEndpointActor(SolrEndPointConfiuration conf) {				                
		String solr_url = String.format("%s/%s", conf.getUrl(), conf.getCore());
		
        this.sw = new SolrWriter(solr_url, conf.getUsername(), conf.getPassword());
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
		
		sw.close();
		getContext().parent().tell(new EndPointFailed(conf), ActorRef.noSender());		
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();
		LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent().path());
	}

	private void send(NewLineEvent s) {
		new RetryCommand(3, s.getPath()).run(new Callable<Void>() {						
			@Override
			public Void call() throws Exception {
				sw.write(new JsonParser().parse(new Gson().toJson(s)).getAsJsonObject());
//				sw.flush();
				LOGGER.info("send {}", s.toString());
				return null;
			}		
		});										
	}		
}	
