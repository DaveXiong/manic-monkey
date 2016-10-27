package com.netflix.simianarmy.client.gcloud;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceList;

public class GCloud {

	public GCloud(String credential) {

		try {
			init(credential);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Compute computeService;

	private void init(String credential) throws Exception {
		FileInputStream inputstream = new FileInputStream(credential);
		GoogleCredential googleCredential = GoogleCredential
				.fromStream(inputstream);

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
					.createScoped(Collections
							.singletonList("https://www.googleapis.com/auth/cloud-platform"));
		}

		HttpTransport httpTransport = GoogleNetHttpTransport
				.newTrustedTransport();
		JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
		computeService = new Compute.Builder(httpTransport, jsonFactory,
				googleCredential).setApplicationName(
				"Google Cloud Platform Sample").build();
	}

	public void stop(String project, String zone, String instanceName)
			throws Exception {
		Compute.Instances.Stop stopRequest = computeService.instances().stop(
				project, zone, instanceName);
		stopRequest.execute();
	}

	public void start(String project, String zone, String instance)
			throws Exception {
		Compute.Instances.Start stopRequest = computeService.instances().start(
				project, zone, instance);
		stopRequest.execute();

	}

	public Instance get(String project, String zone, String instanceName)
			throws Exception {
		Compute.Instances.Get getRequest = computeService.instances().get(
				project, zone, instanceName);

		return getRequest.execute();
	}

	public java.util.List<Instance> list(String project, String zone)
			throws Exception {
		Compute.Instances.List request = computeService.instances().list(
				project, zone);
		InstanceList response;

		java.util.List<Instance> instances = new ArrayList<Instance>();
		do {
			response = request.execute();
			if (response.getItems() == null)
				continue;

			for (Instance instance : response.getItems()) {
				// TODO: Add code here to process each 'instance' resource
				System.out.println(instance.getName() + "-->"
						+ instance.getStatus());
				instances.add(instance);

			}

			request.setPageToken(response.getNextPageToken());
		} while (response.getNextPageToken() != null);

		return instances;
	}

}
