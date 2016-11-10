/**
 * 
 */
package com.netflix.simianarmy.manic;

import com.netflix.simianarmy.client.gcloud.Gce.Status;

/**
 * @author dxiong
 *
 */
public class ManicEvent {

	public static enum Type{
		MONKEY,INSTANCE,SYSTEM
	}
	
	public static enum Command{
		START,
		STOP,
		PAUSE,
		RESUME,
		STATUS_UPDATE
	}
	private Type type;
	private Command event;
	private long createdAt;

	private Payload payload;

	public ManicEvent(Type type, Command command, long createdAt) {
		super();
		this.type = type;
		this.event = command;
		this.createdAt = createdAt;
	}

	public ManicEvent(Type type, Command command) {
		this(type, command, System.currentTimeMillis());
	}

	public Payload getPayload() {
		return payload;
	}

	public void setPayload(Payload payload) {
		this.payload = payload;
	}

	

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Command getCommand() {
		return event;
	}

	public void setCommand(Command command) {
		this.event = command;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	public static interface Payload {

	}

	public static class MonkeyPayload implements Payload {
		private boolean enabled;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

	public static class SystemPayload implements Payload{
		private String message;

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
		
	}
	public static class InstancePayload implements Payload {
		private Status status;
		private Status previousStatus;
		private String name;
		private String group;

		public Status getStatus() {
			return status;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getGroup() {
			return group;
		}

		public void setGroup(String group) {
			this.group = group;
		}

		public Status getPreviousStatus() {
			return previousStatus;
		}

		public void setPreviousStatus(Status previousStatus) {
			this.previousStatus = previousStatus;
		}
		
		

	}
}
