package com.neusoft.rsapm.nilebeat.akka;

import static com.neusoft.rsapm.nilebeat.service.ActorNamesFactory.tailer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.neusoft.rsapm.nilebeat.akka.dto.EndPointFailed;
import com.neusoft.rsapm.nilebeat.akka.dto.Messages;
import com.neusoft.rsapm.nilebeat.akka.dto.NewLineEvent;
import com.neusoft.rsapm.nilebeat.configuration.EndpointConfiguration;
import com.neusoft.rsapm.nilebeat.configuration.EndpointsConfigurationValidator.EndpointsConfiguration;
import com.neusoft.rsapm.nilebeat.configuration.ExportsConfigurationValidator.SingleConfiguration;
import com.neusoft.rsapm.nilebeat.guice.GuiceAbstractActor;
import com.neusoft.rsapm.nilebeat.service.Endpoint;
import com.neusoft.rsapm.nilebeat.util.Util;
import com.neusoft.rsapm.tsp.api.OnlineLoggerProcessAPI;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent.MemberUp;
import akka.routing.Router;
import akka.routing.ActorRefRoutee;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Routee;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.FiniteDuration;

@Slf4j@Getter
public class ManagerActor extends GuiceAbstractActor {	
	private final static String SCHEDULATION_CHECK = "SchedulationsCheck";
	private static OnlineLoggerProcessAPI process;
	private static Path process_path;
	private final Map<ActorRef, List<EndPointFailed>> association;
	private final Cancellable schedule;
	private final Cluster cluster = Cluster.get(getContext().getSystem());
	private final EndpointsConfiguration conf;
	private Router router;
	
	static {
		Config config = ConfigFactory.load("nilebeat");
		ProcessConfiuration processConfiuration = null;
		if (config.hasPath("process")) {
			processConfiuration = new ProcessConfiuration(config.getConfig("process"));
			process_path = processConfiuration.getPath();
		} else {
			process_path = ProcessConfiuration.default_path;
		}
		
		if (process_path.toFile().exists() && process_path.toFile().isFile()){
			try {
				process = Util.open(processConfiuration.getPath());
			} catch (ClassNotFoundException | IOException e) {
				// TODO Auto-generated catch block
				LOGGER.warn(e.getMessage());
				process = new OnlineLoggerProcessAPI();
			}
		} else if (processConfiuration == null) {
			process = new OnlineLoggerProcessAPI();
		} else if (processConfiuration.getQuotations()!=null && processConfiuration.getQuotations()!=null) {
			process = new OnlineLoggerProcessAPI(
					processConfiuration.getWildcard_k(),
					processConfiuration.getMerge_k(),
					processConfiuration.getSpliters(),
					processConfiuration.getQuotations());
		} else {
			process = new OnlineLoggerProcessAPI(
					processConfiuration.getWildcard_k(),
					processConfiuration.getMerge_k());
		}
	}

	@Inject
	public ManagerActor(EndpointsConfiguration conf) {
		this.conf = conf;
		this.association = new HashMap<>();
		
		final ActorSystem system = getContext().system();
		
		if (conf.getBulk().isAvailable()) {
			this.router = new Router(new BulkBroadcastRoutingLogic(conf.getBulk().getSize()));
			
			registerToBulkTimeoutActor();
		} else {
			this.router = new Router(new BroadcastRoutingLogic());			
		}
		
		conf.getEndpoints().forEach(ce -> {
			final Endpoint endpoint = Endpoint.valueOf(ce);
			router = router.addRoutee(buildRoutee(ce, endpoint));
		});
		
		schedule = system.scheduler().schedule(
			FiniteDuration.create(10, TimeUnit.SECONDS), 
			FiniteDuration.create(10, TimeUnit.MINUTES), 
			getSelf(), SCHEDULATION_CHECK, 
			system.dispatcher(), getSelf()
		);
	}
	
	@Override
	public void postStop() throws Exception {
		super.postStop();
		LOGGER.info("end {} ", getSelf().path());
		
		schedule.cancel();
	    cluster.unsubscribe(getSelf());
	}
	
	@Override
	public void preStart() throws Exception {
		super.preStart();		
		LOGGER.info("start {} with parent {}", getSelf().path(), getContext().parent().path());
		
		cluster.subscribe(getSelf(), MemberUp.class);
	}
	
	@Override
	public Receive createReceive() {
		return receiveBuilder()
			.match(EndPointFailed.class, f -> {
				getFailed(getSender()).add(f);
			})
			.matchEquals(SCHEDULATION_CHECK, sc -> {	
				final Set<ActorRef> actorRefs = association.keySet();											
				
				actorRefs.forEach(childActor -> {						
					final List<EndPointFailed> fails = association.get(childActor);
					LOGGER.info("### found {} failed conf for {}", fails.size(), childActor);
					
					for(int i = 0; i < fails.size(); i++) {						
						final EndPointFailed epf = fails.get(i);
						
						if (epf.isExpired()) {										
							childActor.tell(epf, ActorRef.noSender());
							fails.remove(i);
						}						
					}
				});
				
				//remove key's without values!!
				actorRefs.removeAll(
					actorRefs.stream()
						.filter(childActor -> association.get(childActor).isEmpty())
						.collect(Collectors.toList())
				);
				
				Util.save(process_path, process);
			})
			.match(NewLineEvent.class, line -> {
				process.process(line.getContent()).stream().forEachOrdered(j -> {
					JsonObject json = new JsonParser().parse(new Gson().toJson(line)).getAsJsonObject();
					json.entrySet().forEach(e -> {
						j.add(e.getKey(), e.getValue());
					});
					router.route(j, ActorRef.noSender());
				});
			})
//			.match(NewLineEvent.class, l -> {
//				router.route(l, ActorRef.noSender());
//			})
			.match(SingleConfiguration.class, sc -> {
				
				LOGGER.info("SingleConfiguration = {}", sc.getPath());
				
				getContext().actorOf(
					Props.create(TailerActor.class, sc), tailer()
				);

				//XXX this actor should be watched ? 
				//getContext().watch(tailActor);
			})
			.matchEquals(Messages.WISP_REGISTRATION, o -> { //from /user/bulk-timeout				
				getSender().tell(Messages.ANCIENT_REGISTRATION, getSelf());
			})
			.matchEquals(BulkTimeoutActor.BULK_TIMEOUT, o -> { //from /user/bulk-timeout				
				if (conf.getBulk().isAvailable()) {
					router.route(o, ActorRef.noSender());
				}
			})
			.matchAny(o -> {
				LOGGER.warn("not handled message", o);
				unhandled(o);
			})
			.build();
	}

	private List<EndPointFailed> getFailed(ActorRef sender) {
		if (!association.containsKey(sender)) {
			association.put(sender, new ArrayList<>());
		}
		
		return association.get(sender);
	}
	
	private Routee buildRoutee(EndpointConfiguration conf, Endpoint endpoint) {		
		final ActorRef child = getContext().actorOf(
			Props.create(endpoint.getActorClazz(), conf), endpoint.actorName()
		);
		
		getContext().watch(child); //to see Terminated event associated with 'child' actor
		
		return new ActorRefRoutee(child);		
	}
	
	private void registerToBulkTimeoutActor() {
		getContext()
			.actorSelection("/user/bulk-timeout")				
			.tell(conf.getBulk(), getSelf());
	}
	
	@Getter@ToString
	public static class ProcessConfiuration {
		
		private static final Path default_path = 
				Paths.get(System.getProperty("user.dir"), "data", "nilebeat.rsapm");
		
		private static final int default_wildcard_k = 2;
		private static final int default_merge_k = 50;
		
		private Path path = default_path;
		private int wildcard_k = default_wildcard_k;
		private int merge_k = default_merge_k;
		private List<Character> spliters = null;
		private Map<Character, Character> quotations = null;
		
		public ProcessConfiuration(Config config) {
			if (config.hasPath("path")) {
				this.path = Paths.get(config.getString("path"));
			}
			
			if (config.hasPath("wildcard-k")) {
				this.wildcard_k = config.getInt("wildcard-k");
			} 
			if (config.hasPath("merge-k")) {
				this.merge_k = config.getInt("merge-k");
			} 
			
			if (config.hasPath("spliters")) {
				this.spliters = new ArrayList<>();
				config.getConfigList("spliters").stream().filter(c -> {
					return c.getString("spliter").length() == 1;
				}).forEachOrdered(c -> {
					this.spliters.add(c.getString("spliter").charAt(0));
				});
			}
			
			if (config.hasPath("quotations")) {
				this.quotations = new HashMap<>();
				config.getConfigList("quotations").stream().filter(c -> {
					return c.getString("opened").length()==1 && c.getString("closed").length()==1;
				}).forEachOrdered(c -> {
					this.quotations.put(
							c.getString("opened").charAt(0), 
							c.getString("closed").charAt(0));
				});
			}
			
		}
	}
	
}
