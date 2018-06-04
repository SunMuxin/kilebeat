package com.neusoft.aclome.kilebeat.akka;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;

import org.apache.commons.io.IOUtils;

import com.google.inject.Inject;
import com.neusoft.aclome.kilebeat.akka.dto.EndPointFailed;
import com.neusoft.aclome.kilebeat.akka.dto.NewLineEvent;
import com.neusoft.aclome.kilebeat.configuration.GraphiteEndPointConfiuration;
import com.neusoft.aclome.kilebeat.guice.GuiceAbstractActor;

import akka.actor.ActorRef;
import akka.japi.pf.FI;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GraphiteEndpointActor extends GuiceAbstractActor {

	private final GraphiteEndPointConfiuration conf;
	private final Socket socket;
	private final Writer writer;
	
	@Inject
	public GraphiteEndpointActor(GraphiteEndPointConfiuration conf) throws Exception {
		this.conf = conf;
		this.socket = new Socket("localhost", 2003); //XXX make this configurable please!!?
		this.writer = new OutputStreamWriter(socket.getOutputStream());
	}
	
	@Override
	public void postStop() throws Exception {
		super.postStop();		
		LOGGER.info("end {} ", getSelf().path());
		
		IOUtils.closeQuietly(socket);
		IOUtils.closeQuietly(writer);
		
		getContext().parent().tell(new EndPointFailed(conf), ActorRef.noSender());
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();		
		LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent().path());
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(NewLineEvent.class, new FI.UnitApply<NewLineEvent>() {
				@Override
				public void apply(NewLineEvent s) throws Exception { //XXX add some retry ??
					
					if (!socket.isConnected()) {
						throw new RuntimeException("Connection loss with graphite on " + conf);
					}
										
					//see http://graphite.readthedocs.io/en/latest/feeding-carbon.html#getting-your-data-into-graphite					
					writer.write(s.getPath_s() + " " + s.getContent_s().length() +  " " + s.getRs_timestamp() + "\n"); 
					writer.flush();
					
				}
			})
			.matchAny(o -> {
				LOGGER.warn("not handled message", o);
				unhandled(o);
			})			
			.build();
	}
	
}
