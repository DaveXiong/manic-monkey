/**
 * 
 */
package com.netflix.simianarmy.client.gcloud;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.InstanceGroupList;
import com.google.api.services.compute.model.InstanceList;
import com.google.api.services.compute.model.Tags;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.client.gcloud.BasicChaosCrawler.Types;

/**
 * @author dxiong
 *
 */
public class Gce {

	private static final Logger LOGGER = LoggerFactory.getLogger(Gce.class);

	protected String project;
	// protected String zone;
	protected String [] zones;

	protected Compute computeService;

	private Map<String, Instance> name2Instance = new HashMap<String, Instance>();

	public Gce(String credential, String project, String zone) throws Exception {
		this.project = project;
		this.zones = zone.split(",");

		FileInputStream inputstream = new FileInputStream(credential);
		GoogleCredential googleCredential = GoogleCredential.fromStream(inputstream);

		// The createScopedRequired method returns true when running on GAE or a
		// local developer
		// machine. In that case, the desired scopes must be passed in manually.
		// When the code is
		// running in GCE, GKE or a Managed VM, the scopes are pulled from the
		// GCE metadata server.
		// For more information, see
		// https://developers.google.com/identity/protocols/application-default-credentials
		if (googleCredential.createScopedRequired()) {
			googleCredential = googleCredential
					.createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
		}

		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		computeService = new Compute.Builder(httpTransport, jsonFactory, googleCredential)
				.setApplicationName("Google Cloud Platform Sample").build();
	}

	protected List<com.google.api.services.compute.model.Instance> internalList(String zone) throws IOException {
		Compute.Instances.List request = computeService.instances().list(project, zone);
		InstanceList response;

		List<com.google.api.services.compute.model.Instance> instances = new ArrayList<com.google.api.services.compute.model.Instance>();
		do {
			response = request.execute();
			if (response.getItems() == null)
				continue;

			for (com.google.api.services.compute.model.Instance instance : response.getItems()) {
				instances.add(instance);
			}
			request.setPageToken(response.getNextPageToken());
		} while (response.getNextPageToken() != null);

		return instances;
	}

	private String[] getZones() {
		return this.zones;
	}

	public List<Instance> list() throws IOException {

		List<Instance> instances = new ArrayList<Instance>();

		for (String zone : getZones()) {

			for (com.google.api.services.compute.model.Instance instance : internalList(zone)) {
				Instance ins = new Instance(instance.getId().longValue(), instance.getName(),
						Status.parse(instance.getStatus()), zone);

				Tags tag = instance.getTags();
				if (tag != null && tag.getItems() != null) {
					ins.setTags(tag.getItems());
				} else {
					ins.setTags(new ArrayList<String>());
				}

				instances.add(ins);
			}

		}

		synchronized (name2Instance) {
			for (Instance instance : instances) {
				name2Instance.put(instance.getName(), instance);
			}
		}

		return instances;
	}

	public List<InstanceGroup> listGroups() throws IOException {
		List<InstanceGroup> groups = new ArrayList<InstanceGroup>();

		for (String zone : getZones()) {
			Compute.InstanceGroups.List request = computeService.instanceGroups().list(project, zone);
			InstanceGroupList response;
			do {
				response = request.execute();
				if (response.getItems() == null)
					continue;

				for (com.google.api.services.compute.model.InstanceGroup g : response.getItems()) {
					groups.add(new BasicInstanceGroup(g.getName(), Types.ASG, g.getRegion(), null));
				}
				request.setPageToken(response.getNextPageToken());
			} while (response.getNextPageToken() != null);

		}
		return groups;
	}

	public List<Instance> list(String group) throws IOException {

		List<Instance> instances = new ArrayList<Instance>();

		for (String zone : getZones()) {
			Compute.InstanceGroups.ListInstances request = computeService.instanceGroups().listInstances(project, zone,
					group, null);
			com.google.api.services.compute.model.InstanceGroupsListInstances response;

			do {
				response = request.execute();
				if (response.getItems() == null)
					continue;

				for (com.google.api.services.compute.model.InstanceWithNamedPorts instance : response.getItems()) {
					LOGGER.info(instance.getInstance() + ":" + instance.getStatus());
					// TODO
					// instances.add(new Instance(instance.getId().longValue(),
					// instance.getName(), instance.getStatus()));
				}
				request.setPageToken(response.getNextPageToken());
			} while (response.getNextPageToken() != null);

		}

		for (Instance instance : instances) {
			name2Instance.put(instance.getName(), instance);
		}
		return instances;
	}

	public Instance get(String id, String zone) throws IOException {
		Compute.Instances.Get getRequest = computeService.instances().get(project, zone, id);
		com.google.api.services.compute.model.Instance instance = getRequest.execute();
		Instance ins = new Instance(instance.getId().longValue(), instance.getName(),
				Status.parse(instance.getStatus()), zone);
		Tags tag = instance.getTags();
		if (tag != null && tag.getItems() != null) {
			ins.setTags(tag.getItems());
		} else {
			ins.setTags(new ArrayList<String>());
		}
		return ins;
	}

	public Instance get(String id) throws IOException {
		Instance instance = name2Instance.get(id);
		if (instance != null) {
			return get(id, instance.getZone());
		} else {
			for (String zone : getZones()) {
				try {
					Instance in = get(id, zone);
					if (in != null) {
						return instance;
					}
				} catch (IOException ex) {

				}
			}
		}

		throw new RuntimeException("Can't find instance " + id);

	}

	public Instance stop(String id, String zone) throws IOException {
		LOGGER.info("stop {} in project:{}, zone:{}", new Object[] { id, project, zone });
		Compute.Instances.Stop stopRequest = computeService.instances().stop(project, zone, id);
		stopRequest.execute();

		return get(id, zone);
	}

	public Instance stop(String id) throws IOException {

		Instance instance = name2Instance.get(id);
		if (instance != null) {
			return stop(id, instance.getZone());
		}

		for (String zone : getZones()) {
			try {
				Instance inst = stop(id, zone);
				if (inst != null) {
					return inst;
				}
			} catch (IOException ex) {

			}
		}

		throw new RuntimeException("Can't find instance " + id);
	}

	public Instance start(String id, String zone) throws IOException {
		LOGGER.info("start {} in project:{}, zone:{}", new Object[] { id, project, zone });

		Compute.Instances.Start stopRequest = computeService.instances().start(project, zone, id);
		stopRequest.execute();

		return get(id, zone);
	}

	public Instance start(String id) throws IOException {
		Instance instance = name2Instance.get(id);
		if (instance != null) {
			return start(id, instance.getZone());
		}
		for (String zone : getZones()) {
			try {
				Instance inst = start(id, zone);
				if (inst != null) {
					return inst;
				}
			} catch (IOException ex) {

			}
		}

		throw new RuntimeException("Can't find instance " + id);
	}

	public Instance suspend(String id) throws IOException {

		throw new UnsupportedOperationException();
	}

	public Instance resume(String id) throws IOException {

		throw new UnsupportedOperationException();
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	// public String getZone() {
	// return zone;
	// }
	//
	// public void setZone(String zone) {
	// this.zone = zone;
	// }

	public static enum Status {
		PROVISIONING, STAGING, RUNNING, STOPPING, SUSPENDING, SUSPENDED, TERMINATED, UNKNOWN;
		public static Status parse(String name) {
			for (Status status : Status.values()) {
				if (status.toString().equalsIgnoreCase(name)) {
					return status;
				}
			}

			return UNKNOWN;
		}
	}

	public static class Instance {
		private long id;
		private String name;
		private Status status;
		private List<String> tags;
		private String zone;

		public Instance(Long id, String name, Status status, String zone) {
			this.id = id;
			this.name = name;
			this.status = status;
			this.zone = zone;
		}

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Status getStatus() {
			return status;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public List<String> getTags() {
			return tags;
		}

		public void setTags(List<String> tags) {
			this.tags = tags;
		}

		public String getZone() {
			return zone;
		}

		public void setZone(String zone) {
			this.zone = zone;
		}

		@Override
		public String toString() {
			return "Instance [id=" + id + ", name=" + name + ", status=" + status + ", tags=" + tags + ", zone=" + zone
					+ "]";
		}

	}

	public static void main(String[] args) throws Exception {
//		Gce gce = new Gce("/home/dxiong/Downloads/google-cloud-sdk/dxiong-iix-dev.json", "iix-dev-environment",
//				"us-central1-a");
//		for (Instance instance : gce.list()) {
//			System.out.println(instance);
//		}
		
		List<String> list = Arrays.asList("1,2,3");
		for(String a:list) {
			System.out.println(a);;
		}
		for(String str:list.toArray(new String[list.size()])){
			System.out.println(str);
		}
	}

}
