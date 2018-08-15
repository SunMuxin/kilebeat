package com.neusoft.rsapm.nilebeat.akka;

import static akka.actor.SupervisorStrategy.stop;

import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import com.neusoft.rsapm.nilebeat.akka.dto.JobFailed;
import com.neusoft.rsapm.nilebeat.akka.dto.Messages;
import com.neusoft.rsapm.nilebeat.akka.dto.NewLineEvent;
import com.neusoft.rsapm.nilebeat.configuration.ExportsConfigurationValidator.SingleConfiguration;
import com.neusoft.rsapm.nilebeat.guice.GuiceAbstractActor;

import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.PoisonPill;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.cluster.Cluster;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.cluster.ClusterEvent.CurrentClusterState;
import akka.cluster.ClusterEvent.MemberUp;
import akka.japi.pf.DeciderBuilder;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.Duration;

@Slf4j
public class TailerActor extends GuiceAbstractActor implements TailerListener {

	private static final int DELAY = 50;
	private static final int BUFFER_SIZE = 1000;
	private static final boolean FROM_END = true;
	private static final boolean RE_OPEN = true;

	private final SingleConfiguration conf;
	private final Tailer tailer;
	private final Cluster cluster = Cluster.get(getContext().system());
	private final List<ActorRef> ancients = new ArrayList<ActorRef>();

	private int jobCounter = 0;

	public TailerActor(SingleConfiguration conf) {
		this.conf = conf;

		this.tailer = Tailer.create(conf.getPath(), Charset.forName("UTF-8"), this, DELAY, FROM_END, RE_OPEN,
				BUFFER_SIZE);
	}

	@Override
	public void postStop() throws Exception {
		super.postStop();
		LOGGER.info("end {} ", getSelf().path());

		tailer.stop();
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
				.match(NewLineEvent.class, job -> { // from self
					// LOGGER.info("[row] {}", s);
					if (conf.getRules().mustBeSent(job.getContent()) && ancients.isEmpty()) {
						getSender().tell(
								new JobFailed("Service unavailable, try again later", job), 
								getSender());
						LOGGER.info("JobFailed {}", job.getContent());
					} else if (conf.getRules().mustBeSent(job.getContent())) {
						jobCounter++;
						ancients.get(jobCounter % ancients.size()).forward(job, getContext());
					}
				})
				.match(Terminated.class, t -> { // from any childs
					final ActorRef fail = t.actor();
					LOGGER.warn("actor {} is terminated", fail.path());
		
					ancients.remove(t.getActor());
		
					getContext().unwatch(fail);
				})
				.match(CurrentClusterState.class, s -> {
					s.allRoles();
					s.getMembers().forEach(m -> {
						if (m.status().equals(MemberStatus.up())) {
					          register(m);
						}
					});
				})
				.match(MemberUp.class, m -> {
					register(m.member());
				})
				.matchEquals(Messages.ANCIENT_REGISTRATION, m -> {
					ancients.add(getSender());
					System.out.println(ancients);
				})
				.matchAny(o -> {
					LOGGER.warn("not handled message", o);
					unhandled(o);
				}).build();
	}

	@Override
	public void init(Tailer tailer) {
	} /* avoid interface segregation please !!? */

	@Override
	public void fileNotFound() {
	} /* avoid interface segregation please !!? */

	@Override
	public void fileRotated() {
	} /* avoid interface segregation please !!? */

	@Override
	public void handle(String line) {
		getSelf().tell(new NewLineEvent(line, conf.getPath().toPath()), ActorRef.noSender());
	}

	@Override
	public void handle(Exception ex) {
		LOGGER.error("[ => ]", ex);

		if (ex instanceof FileNotFoundException) { // occur when file is deleted
													// during tailer are working
													// on!!
			LOGGER.info("file to tail not found {}", conf.getPath());

			getSelf().tell(PoisonPill.getInstance(), ActorRef.noSender());
		}
	}

	@Override
	public SupervisorStrategy supervisorStrategy() {
		// strategy is applied only to the child actor that failed
		return new OneForOneStrategy(1, Duration.create(1, TimeUnit.MINUTES),
				DeciderBuilder.match(Exception.class, e -> stop())
						// match(ArithmeticException.class, e -> resume()).
						// match(NullPointerException.class, e -> restart()).
						// match(IllegalArgumentException.class, e -> stop()).
						// matchAny(o -> escalate())
						.build());
	}
	
	void register(Member member) {
		if (member.hasRole("ancient")) {
			getContext().actorSelection(member.address() + "/user/manager").tell(Messages.WISP_REGISTRATION, getSelf());
		}
	}
}
