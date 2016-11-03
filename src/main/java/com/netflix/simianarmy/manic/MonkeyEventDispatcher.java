/**
 * 
 */
package com.netflix.simianarmy.manic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author dxiong
 *
 */
public class MonkeyEventDispatcher {

	public static final MonkeyEventDispatcher INSTANCE = new MonkeyEventDispatcher();
	
	 private final ScheduledExecutorService scheduler;
	
	private MonkeyEventDispatcher(){
		scheduler = Executors.newScheduledThreadPool(1);

	}
	private List<EventListener> listeners = Collections.synchronizedList(new ArrayList<EventListener>());

	public void dispatch(final ManicEvent event) {
		
		scheduler.execute(new Runnable(){
			@Override
			public void run() {
				for(EventListener listener:listeners){
					listener.onEvent(event);
				}				
			}
			
		});
		
	}

	public void subscribe(EventListener listener) {
		listeners.add(listener);
	}

	public void unsubscribe(EventListener listener) {
		listeners.remove(listener);
	}
}
