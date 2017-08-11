/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.client.gcloud.BasicChaosCrawler.Types;
import com.netflix.simianarmy.client.gcloud.Gce.Instance;
import com.netflix.simianarmy.manic.ManicEvent.InstancePayload;
import com.netflix.simianarmy.manic.ManicEvent.SystemPayload;

/**
 * @author dxiong
 *
 */
public class InstanceMonitor implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMonitor.class);

	private Map<String, InstanceGroup> name2Group = Collections.synchronizedMap(new HashMap<String, InstanceGroup>());
	private Map<String, Instance> name2Instance = Collections.synchronizedMap(new HashMap<String, Instance>());
	private HashMap<String, ScheduledFuture<?>> futures = new HashMap<String, ScheduledFuture<?>>();

	/** The scheduler. */
	private final ScheduledExecutorService scheduler;

	private ManicGceClient client;

	private ManicChaosMonkey monkey;

	public InstanceMonitor(ManicChaosMonkey monkey) {
		this.monkey = monkey;
		this.client = (ManicGceClient) monkey.context().cloudClient();
		scheduler = Executors.newScheduledThreadPool(1);
		this.client.setMonitor(this);
	}

	private Map<String, List<Instance>> group2Instances() throws IOException {

		LOGGER.info("sync group and instances...");
		Map<String, List<Instance>> group2Instances = new HashMap<String, List<Instance>>();

		Set<String> tags = new HashSet<String>();

		List<Instance> allInstances = client.list();

		for (Instance instance : allInstances) {
			
			for (String tag : instance.getTags()) {

				InstanceGroup group = new BasicInstanceGroup(tag, Types.TAG, instance.getZone(), null);

				if (monkey.isGroupEnabled(group)) {
					
					tags.add(tag);
					List<Instance> instances = group2Instances.get(tag);
					if (instances == null) {
						instances = new ArrayList<Instance>();
						group2Instances.put(tag, instances);
					}

					instances.add(instance);
				}
			}
		}

		name2Group.clear();

		for (String tag : tags) {
			name2Group.put(tag, new BasicInstanceGroup(tag, Types.TAG, null, null));
		}

		LOGGER.info("all enabled groups:" + tags);
		LOGGER.info("tag2Instances:"+group2Instances);

		return group2Instances;
	}

	public void run() {

		try {
			Map<String, List<Instance>> group2Instances = group2Instances();

			for (String group : group2Instances.keySet()) {

				for (Instance instance : group2Instances.get(group)) {

					Instance existingInstance = name2Instance.get(instance.getName());

					if (existingInstance == null) {
						existingInstance = instance;
					}
					name2Instance.put(instance.getName(), instance);

					boolean statusUpdated = existingInstance.getStatus() != instance.getStatus();

					if (statusUpdated) {
						ManicEvent event = new ManicEvent(ManicEvent.Type.INSTANCE, ManicEvent.Command.STATUS_UPDATE);
						InstancePayload payload = new InstancePayload();
						payload.setGroup(group);
						payload.setName(instance.getName());
						payload.setStatus(instance.getStatus());
						payload.setPreviousStatus(existingInstance.getStatus());
						payload.setRegion(instance.getZone());
						event.setPayload(payload);
						MonkeyEventDispatcher.INSTANCE.dispatch(event);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();

			ManicEvent event = new ManicEvent(ManicEvent.Type.SYSTEM, ManicEvent.Command.STATUS_UPDATE);
			SystemPayload payload = new SystemPayload();
			payload.setMessage(ex.getMessage());
			event.setPayload(payload);
			MonkeyEventDispatcher.INSTANCE.dispatch(event);
		}

	}

	public void start() {
		futures.put(UUID.randomUUID().toString(), scheduler.scheduleWithFixedDelay(this, 0, 10, TimeUnit.SECONDS));
	}

	public void start(Runnable runnable) {
		scheduler.execute(runnable);
	}

	public void stop() {
		for (String uuid : futures.keySet()) {
			futures.get(uuid).cancel(true);
		}
	}

	public List<InstanceGroup> groups() {
		List<InstanceGroup> groups = new ArrayList<InstanceGroup>();
		groups.addAll(name2Group.values());
		return groups;
	}

	public List<Instance> instances(String group) {
		List<Instance> instances = new ArrayList<Instance>();
		for (Instance instance : name2Instance.values()) {
			if (instance.getTags().contains(group)) {
				instances.add(instance);
			}
		}
		return instances;
	}
}
