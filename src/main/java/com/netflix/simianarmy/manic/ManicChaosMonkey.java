/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.FeatureNotEnabledException;
import com.netflix.simianarmy.InstanceGroupNotFoundException;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.ChaosInstance;
import com.netflix.simianarmy.chaos.ChaosType;
import com.netflix.simianarmy.chaos.SshConfig;

import console.mw.monitor.slack.SlackMessage;

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

	private ManicInstanceSelector instanceSelector;

	private Map<String, ChaosType> instance2Rollback = new HashMap<String, ChaosType>();

	public ManicChaosMonkey(Context ctx) {
		super(ctx);

		allChaosTypes.add(new StopInstanceChaosType(cfg));
		allChaosTypes.add(new StartInstanceChaosType(cfg));

		paused = ctx.configuration().getBoolOrElse(NS + "paused", true);

		monitor = new InstanceMonitor(this);

		slack = new Slack(this);

		instanceSelector = new ManicInstanceSelector(this);

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

	public synchronized void monkeyTimeChanged(boolean isMonkeyTime, int openHour, int closeHour) {

		LOGGER.info("isMonkeyTime:" + isMonkeyTime + ",working hours[" + openHour + "," + closeHour + "]");
		SlackMessage message = new SlackMessage();
		String title = "";
		if (isMonkeyTime) {
			title = "Now is monkey time,work hours[" + openHour + "," + closeHour + "]";
		} else {
			title = "Now is not monkey time,work hours[" + openHour + "," + closeHour + "]";
		}
		message.setText(title);
		slack.sendToSlack(message);

		if (!isMonkeyTime) {
			// rollback all services to original status
			for (String key : instance2Rollback.keySet()) {
				ChaosType chaosType = instance2Rollback.get(key);
				SshConfig sshConfig = new SshConfig(context().configuration());

				ChaosInstance instance = new ChaosInstance(context().cloudClient(), key, sshConfig);
				if (chaosType.canApply(instance)) {
					chaosType.apply(instance);
				}
			}
			
			instance2Rollback.clear();
		}
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

	protected ChaosType pickChaosType(CloudClient cloudClient, InstanceGroup group) {
		Random random = new Random();

		SshConfig sshConfig = new SshConfig(cfg);

		List<ChaosType> applicable = Lists.newArrayList();
		for (ChaosType chaosType : allChaosTypes) {
			for (String instanceId : group.instances()) {
				ChaosInstance instance = new ChaosInstance(cloudClient, instanceId, sshConfig);
				if (chaosType.isEnabled() && chaosType.canApply(instance)) {
					applicable.add(chaosType);
					break;
				}
			}
		}

		if (applicable.isEmpty()) {
			return null;
		}

		int index = random.nextInt(applicable.size());
		return applicable.get(index);
	}

	protected ChaosType pickChaosType(String key) {
		for (ChaosType chaosType : allChaosTypes) {
			if (chaosType.getKey().equalsIgnoreCase(key)) {
				return chaosType;
			}
		}
		return null;

	}

	public void doMonkeyBusiness() {
		context().resetEventReport();
		cfg.reload();
		if (!isChaosMonkeyEnabled()) {
			return;
		}
		for (InstanceGroup group : context().chaosCrawler().groups()) {
			if (isGroupEnabled(group)) {

				ChaosType chaosType = pickChaosType(context().cloudClient(), group);

				if (chaosType == null) {
					// This is surprising ... normally we can always just
					// terminate it
					LOGGER.warn("No chaos type was applicable to the group: {}", group.name());
					continue;
				}

				Collection<String> instances = instanceSelector.select(group, chaosType);
				for (String inst : instances) {
					if (terminateInstance(group, inst, chaosType) != null) {
						if (!instance2Rollback.containsKey(inst)) {
							switch (chaosType.getKey()) {
							case "StartInstance":
								instance2Rollback.put(inst, pickChaosType("ShutdownInstance"));
								break;
							case "ShutdownInstance":
								instance2Rollback.put(inst, pickChaosType("StartInstance"));
								break;
							}
						}
					}
				}
			}
		}
	}

	public boolean isGroupEnabled(InstanceGroup group) {
		return getBoolFromCfgOrDefault(group, "enabled", false);
	}

	protected boolean isMaxTerminationCountExceeded(InstanceGroup group) {
		return false;
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
