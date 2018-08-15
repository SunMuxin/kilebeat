package com.neusoft.rsapm.nilebeat.akka;

import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.neusoft.rsapm.nilebeat.akka.dto.EndPointFailed;
import com.neusoft.rsapm.nilebeat.akka.dto.NewLineEvent;
import com.neusoft.rsapm.nilebeat.configuration.KafkaEndPointConfiuration;
import com.neusoft.rsapm.nilebeat.guice.GuiceAbstractActor;
import com.neusoft.rsapm.nilebeat.retry.RetryCommand;

import akka.actor.ActorRef;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaEndpointActor extends GuiceAbstractActor {
	
	private final ObjectMapper om;
	private final Producer<String, String> producer;	
	private final KafkaEndPointConfiuration conf;
	
	@Inject
	public KafkaEndpointActor(KafkaEndPointConfiuration conf) {				                
        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, conf.getHost());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
		
        this.producer = new KafkaProducer<String, String>(props);				    
        this.om = new ObjectMapper();
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
		
		producer.close();
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
				long offset = producer.send(
					new ProducerRecord<String, String>(conf.getQueue(), om.writeValueAsString(s))
				).get().offset();
							
				LOGGER.info("offset is {}", offset);
				return null;
			}		
		});										
	}		
}	
