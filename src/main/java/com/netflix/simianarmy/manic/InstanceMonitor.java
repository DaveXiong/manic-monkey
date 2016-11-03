/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.client.gcloud.BasicClient;
import com.netflix.simianarmy.client.gcloud.Gce.Instance;
import com.netflix.simianarmy.client.gcloud.Gce.Status;
import com.netflix.simianarmy.manic.ManicEvent.InstancePayload;

/**
 * @author dxiong
 *
 */
public class InstanceMonitor implements Runnable{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(InstanceMonitor.class);

	
	private Map<String,Status> instance2status = Collections.synchronizedMap(new HashMap<String,Status>());
    private HashMap<String, ScheduledFuture<?>> futures = new HashMap<String, ScheduledFuture<?>>();

	 /** The scheduler. */
    private final ScheduledExecutorService scheduler;
    private ManicChaosMonkey monkey;
	public InstanceMonitor(ManicChaosMonkey monkey){
		scheduler = Executors.newScheduledThreadPool(1);
		this.monkey =monkey;
	}
	
	private boolean isStatusUpdated(String instance,Status status){
		if(!instance2status.containsKey(instance)){
			instance2status.put(instance, status);
		}
		
		if(instance2status.get(instance) == status){
			return false;
		}
		
		LOGGER.info("{} status changed from {} to {} ",instance,instance2status.get(instance),status);
		instance2status.put(instance, status);
		return true;
	}
	
	public void run(){
	
		BasicClient client = (BasicClient)(monkey.context().cloudClient());
		
		try {
			
			for(InstanceGroup group:client.listGroups()){
				if(monkey.isGroupEnabled(group)){
					for(Instance instance:client.list(group.name())){
						Status oldStatus = instance2status.get(instance.getName());
						if(isStatusUpdated(instance.getName(),instance.getStatus())){
							ManicEvent event = new ManicEvent(ManicEvent.Type.INSTANCE,ManicEvent.Command.STAUTS_UPDATE);
							InstancePayload payload = new InstancePayload();
							payload.setGroup(group.name());
							payload.setName(instance.getName());
							payload.setStatus(instance.getStatus());
							payload.setPreviousStatus(oldStatus);
							event.setPayload(payload);
							MonkeyEventDispatcher.INSTANCE.dispatch(event);
						}
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void start(){
		futures.put(UUID.randomUUID().toString(), scheduler.scheduleWithFixedDelay(this, 0, 1, TimeUnit.MINUTES));	
	}
	
	public void start(Runnable runnable){
		scheduler.execute(runnable);
	}
	
	public void stop(){
		for(String uuid:futures.keySet()){
			futures.get(uuid).cancel(true);
		}
	}
}
