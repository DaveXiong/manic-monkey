/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.FeatureNotEnabledException;
import com.netflix.simianarmy.InstanceGroupNotFoundException;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.ChaosType;
import com.netflix.simianarmy.chaos.ShutdownInstanceChaosType;
import com.netflix.simianarmy.client.gcloud.BasicClient;
import com.netflix.simianarmy.client.gcloud.Gce;

/**
 * @author dxiong
 *
 */
public class ManicChaosMonkey extends BasicChaosMonkey {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ManicChaosMonkey.class);

	private boolean paused = false;
	
	private InstanceMonitor monitor;
	
	private Slack slack;

	public ManicChaosMonkey(Context ctx) {
		super(ctx);

		allChaosTypes.add(new StopInstanceChaosType(cfg));
		allChaosTypes.add(new StartInstanceChaosType(cfg));

		paused = ctx.configuration().getBoolOrElse(NS + "paused", true);

		monitor = new InstanceMonitor(this);
		
		slack = new Slack(this);
		
		LOGGER.info("Manic Monkey is ready, version:" + Definitions.VERSION);
	}

	public enum ManicEventTypes implements EventType {
		MONKEY_START, MONKEY_STOP, MONKEY_PAUSE, MONKEY_RESUME
	}

	public boolean isChaosMonkeyEnabled() {
		return super.isChaosMonkeyEnabled() && !paused;
	}

	public synchronized void pause() {
		paused = true;
		context().recorder().recordEvent(context().recorder().newEvent(this.type(), ManicEventTypes.MONKEY_PAUSE, null,
				UUID.randomUUID().toString()));
		LOGGER.info("Pause:" + this);

	}

	public synchronized void resume() {
		paused = false;
		context().recorder().recordEvent(context().recorder().newEvent(this.type(), ManicEventTypes.MONKEY_RESUME, null,
				UUID.randomUUID().toString()));
		LOGGER.info("Resume:" + this);
	}

	public synchronized boolean isPaused() {
		return this.paused;
	}

	public synchronized void start() {
		
		context().recorder().recordEvent(context().recorder().newEvent(this.type(), ManicEventTypes.MONKEY_START, null,
				UUID.randomUUID().toString()));
		super.start();
		
		monitor.start();
	}

	public synchronized void stop() {
		context().recorder().recordEvent(context().recorder().newEvent(this.type(), ManicEventTypes.MONKEY_STOP, null,
				UUID.randomUUID().toString()));
		super.stop();
		
		monitor.stop();
	}

	public boolean isGroupEnabled(InstanceGroup group) {
		return super.isGroupEnabled(group);
	}

	protected Event terminateInstance(InstanceGroup group, String inst, ChaosType chaosType) {

		if (chaosType instanceof ShutdownInstanceChaosType) {
			if (!isAllowedToShutdownInstance(group)) {
				reportEventForSummary(EventTypes.CHAOS_TERMINATION_SKIPPED, group, inst);
				return null;
			}
		}

		return super.terminateInstance(group, inst, chaosType);
	}

	protected boolean isAllowedToShutdownInstance(InstanceGroup group) {

		List<String> instances = group.instances();
		if (instances.isEmpty()) {
			LOGGER.info("{} has no any instances", group.name());
			return false;
		}

		if (this.context().cloudClient() instanceof BasicClient) {
			List<String> activeInstances = new ArrayList<String>();
			BasicClient gceClient = (BasicClient) this.context().cloudClient();
			for (String instanceName : instances) {
				try {
					if (gceClient.get(instanceName).getStatus() == Gce.Status.RUNNING) {
						activeInstances.add(instanceName);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			String propName = "minActiveInstances";
			int minActiveInstances = (int) getNumFromCfgOrDefault(group, propName, 1.0);

			LOGGER.info("{} active instances in group {},required minActiveInstances:{}", activeInstances.size(),
					group.name(), minActiveInstances);

			return activeInstances.size() > minActiveInstances;
		}

		return true;

	}

	public Event terminateNow(String type, String name, ChaosType chaosType, String instance)
			throws FeatureNotEnabledException, InstanceGroupNotFoundException {
		Validate.notNull(type);
		Validate.notNull(name);
		cfg.reload(name);
		if (!super.isChaosMonkeyEnabled()) {
			String msg = String.format("Chaos monkey is not enabled for group %s [type %s]", name, type);
			LOGGER.info(msg);
			throw new FeatureNotEnabledException(msg);
		}
		String prop = NS + "terminateOndemand.enabled";
		if (cfg.getBool(prop)) {
			InstanceGroup group = findInstanceGroup(type, name);
			if (group == null) {
				throw new InstanceGroupNotFoundException(type, name);
			}

			return terminateInstance(group, instance, chaosType);

		} else {
			String msg = String.format("Group %s [type %s] does not allow on-demand termination, set %s=true", name,
					type, prop);
			LOGGER.info(msg);
			throw new FeatureNotEnabledException(msg);
		}
	}

	protected void reportEventForSummary(EventTypes eventType, InstanceGroup group, String instanceId) {
		super.reportEventForSummary(eventType, group, instanceId);

	}

	public Slack getSlack() {
		return slack;
	}

	public void setSlack(Slack slack) {
		this.slack = slack;
	}

	public InstanceMonitor getMonitor() {
		return monitor;
	}

	public void setMonitor(InstanceMonitor monitor) {
		this.monitor = monitor;
	}
	
	

	
}
