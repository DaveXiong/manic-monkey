/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.simianarmy.resources.manic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.FeatureNotEnabledException;
import com.netflix.simianarmy.InstanceGroupNotFoundException;
import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.NotFoundException;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.chaos.ChaosType;
import com.netflix.simianarmy.client.gcloud.BasicChaosCrawler;
import com.netflix.simianarmy.client.gcloud.BasicClient;
import com.netflix.simianarmy.client.gcloud.Gce.Instance;
import com.netflix.simianarmy.manic.Definitions;
import com.netflix.simianarmy.manic.ManicChaosMonkey;
import com.sun.jersey.spi.resource.Singleton;

/**
 * The Class ManicMonkeyResource for json REST apis.
 */
@Path("/v1/manic")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class ManicMonkeyResource {

	/** The Constant JSON_FACTORY. */
	private static final MappingJsonFactory JSON_FACTORY = new MappingJsonFactory();

	/** The monkey. */
	private ManicChaosMonkey monkey = null;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ManicMonkeyResource.class);

	/**
	 * Instantiates a chaos monkey resource with a specific chaos monkey.
	 *
	 * @param monkey
	 *            the chaos monkey
	 */
	public ManicMonkeyResource(ManicChaosMonkey monkey) {
		this.monkey = monkey;
	}

	/**
	 * Instantiates a chaos monkey resource using a registered chaos monkey from
	 * factory.
	 */
	public ManicMonkeyResource() {
		for (Monkey runningMonkey : MonkeyRunner.getInstance().getMonkeys()) {
			if (runningMonkey instanceof ManicChaosMonkey) {
				this.monkey = (ManicChaosMonkey) runningMonkey;
				break;
			}
		}
		if (this.monkey == null) {
			LOGGER.info("Creating a new ManicMonkey monkey instance for the resource.");
			this.monkey = MonkeyRunner.getInstance().factory(ManicChaosMonkey.class);
		}
	}

	@GET
	public Response getHeartBeat() throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);

		gen.writeStartObject();
		gen.writeStringField("version", Definitions.VERSION);
		gen.writeBooleanField("enabled", monkey.isChaosMonkeyEnabled());
		long now = System.currentTimeMillis();
		gen.writeNumberField("now", now);
		gen.writeNumberField("uptime", now - Definitions.UP_AT);
		gen.writeArrayFieldStart("actions");
		for (ChaosType type : monkey.getChaosTypes()) {
			gen.writeStartObject();
			gen.writeStringField("name", type.getKey());
			gen.writeBooleanField("enabled", type.isEnabled());
			gen.writeEndObject();
		}
		gen.writeEndArray();

		gen.writeEndObject();

		gen.close();
		return Response.status(Response.Status.OK).entity(baos.toString("UTF-8")).build();
	}

	/**
	 * Gets the chaos events. Creates GET /api/v1/chaos api which outputs the
	 * chaos events in json. Users can specify cgi query params to filter the
	 * results and use "since" query param to set the start of a timerange.
	 * "since" should be specified in milliseconds since the epoch.
	 *
	 * @param uriInfo
	 *            the uri info
	 * @return the chaos events json response
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	@GET
	@Path("/events")
	public Response getEvents(@Context UriInfo uriInfo) throws IOException {
		Map<String, String> query = new HashMap<String, String>();
		Date date = null;
		for (Map.Entry<String, List<String>> pair : uriInfo.getQueryParameters().entrySet()) {
			if (pair.getValue().isEmpty()) {
				continue;
			}
			if (pair.getKey().equals("since")) {
				date = new Date(Long.parseLong(pair.getValue().get(0)));
			} else {
				query.put(pair.getKey(), pair.getValue().get(0));
			}
		}
		// if "since" not set, default to 24 hours ago
		if (date == null) {
			Calendar now = monkey.context().calendar().now();
			now.add(Calendar.DAY_OF_YEAR, -1);
			date = now.getTime();
		}

		List<Event> evts = monkey.context().recorder().findEvents(ChaosMonkey.Type.CHAOS,
				ChaosMonkey.EventTypes.CHAOS_TERMINATION, query, date);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);
		gen.writeStartObject();
		gen.writeArrayFieldStart("results");
		for (Event evt : evts) {
			gen.writeStartObject();
			gen.writeStringField("instance", evt.id());
			gen.writeNumberField("createdAt", evt.eventTime().getTime());

			Map<String, String> fields = evt.fields();

			gen.writeStringField("group", fields.getOrDefault("groupName", ""));
			gen.writeStringField("type", fields.getOrDefault("chaosType", ""));

			gen.writeEndObject();
		}
		gen.writeEndArray();
		gen.writeEndObject();
		gen.close();
		return Response.status(Response.Status.OK).entity(baos.toString("UTF-8")).build();
	}

	@GET
	@Path("/group")
	public Response getInstances() throws IOException {

		List<InstanceGroup> groups = monkey.context().chaosCrawler().groups();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);

		gen.writeStartObject();
		gen.writeArrayFieldStart("results");
		for (InstanceGroup group : groups) {
			gen.writeStartObject();
			gen.writeStringField("name", group.name());
			gen.writeBooleanField("enabled", monkey.isGroupEnabled(group));

			if (monkey.context().cloudClient() instanceof BasicClient) {
				BasicClient client = (BasicClient) monkey.context().cloudClient();
				gen.writeArrayFieldStart("instances");
				for (Instance instance : client.list(group.name())) {
					gen.writeStartObject();
					gen.writeStringField("name", instance.getName());
					gen.writeStringField("status", instance.getStatus().toString());
					gen.writeStringField("region", instance.getZone());
					gen.writeEndObject();
				}
				gen.writeEndArray();
			}

			gen.writeEndObject();
		}
		gen.writeEndArray();
		gen.writeEndObject();

		gen.close();
		return Response.status(Response.Status.OK).entity(baos.toString("UTF-8")).build();
	}

	@GET
	@Path("/group/{group}")
	public Response getInstances(@PathParam("group") String group) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);

		InstanceGroup groupInstance = new BasicInstanceGroup(group, BasicChaosCrawler.Types.TAG, null, null);

		if (!monkey.isGroupEnabled(groupInstance)) {

			gen.writeStartObject();
			gen.writeStringField("error", "group(" + group + ") is not enabled");
			gen.writeEndObject();

			return Response.status(Response.Status.BAD_REQUEST).entity(baos.toString("UTF-8")).build();
		}

		BasicClient client = (BasicClient) monkey.context().cloudClient();

		List<Instance> instances = client.list(group);

		gen.writeStartObject();
		gen.writeBooleanField("enabled", monkey.isGroupEnabled(groupInstance));
		gen.writeArrayFieldStart("results");
		for (Instance instance : instances) {
			gen.writeStartObject();
			gen.writeStringField("name", instance.getName());
			gen.writeStringField("status", instance.getStatus().name());
			gen.writeStringField("region", instance.getZone());
			gen.writeEndObject();
		}
		gen.writeEndArray();
		gen.writeEndObject();

		gen.close();
		return Response.status(Response.Status.OK).entity(baos.toString("UTF-8")).build();
	}

	@GET
	@Path("/group/{group}/{instance}")
	public Response getInstance(@PathParam("group") String group, @PathParam("instance") String id) throws Exception {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);

		InstanceGroup groupInstance = new BasicInstanceGroup(group, BasicChaosCrawler.Types.TAG, null, null);

		if (!monkey.isGroupEnabled(groupInstance)) {

			gen.writeStartObject();
			gen.writeStringField("error", "group(" + group + ") is not enabled");
			gen.writeEndObject();

			return Response.status(Response.Status.BAD_REQUEST).entity(baos.toString("UTF-8")).build();
		}

		BasicClient client = (BasicClient) monkey.context().cloudClient();
		try {
			Instance instance = client.get(id);

			gen.writeStartObject();
			gen.writeStringField("name", instance.getName());
			gen.writeStringField("status", instance.getStatus().name());
			gen.writeStringField("region",instance.getZone());
			gen.writeEndObject();

			gen.close();

			return Response.status(Response.Status.OK).entity(baos.toString("UTF-8")).build();

		} catch (IOException ex) {
			ex.printStackTrace();
			gen.writeStartObject();
			gen.writeStringField("error", ex.getMessage());
			gen.writeEndObject();

			gen.close();
			return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(baos.toString("UTF-8")).build();

		}
	}

	public static enum MonkeyAction {
		pause, resume
	}

	@POST
	@Path("/{action}")
	public Response pauseAndResumeMonkey(@PathParam("action") MonkeyAction action) throws IOException {

		switch (action) {
		case pause:
			this.monkey.pause();
			break;
		case resume:
			this.monkey.resume();
			break;
		default:
			return Response.status(Response.Status.BAD_REQUEST).build();

		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);

		gen.writeStartObject();
		gen.writeStringField("version", Definitions.VERSION);
		gen.writeBooleanField("enabled", monkey.isChaosMonkeyEnabled());
		long now = System.currentTimeMillis();
		gen.writeNumberField("now", now);
		gen.writeNumberField("uptime", now - Definitions.UP_AT);

		gen.writeEndObject();

		gen.close();
		return Response.status(Response.Status.OK).entity(baos.toString("UTF-8")).build();
	}

	public static enum InstanceAction {
		start, stop, suspend, resume
	}

	@POST
	@Path("/group/{group}/{action}")
	public Response controlInstances(@PathParam("group") String group, @PathParam("action") InstanceAction action)
			throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);

		InstanceGroup groupInstance = new BasicInstanceGroup(group, BasicChaosCrawler.Types.TAG, null, null);

		if (!monkey.isGroupEnabled(groupInstance)) {

			gen.writeStartObject();
			gen.writeStringField("error", "group(" + group + ") is not enabled");
			gen.writeEndObject();

			return Response.status(Response.Status.BAD_REQUEST).entity(baos.toString("UTF-8")).build();
		}
		
		String chaosTypeName = null;
		switch (action) {
		case start:
			chaosTypeName = "StartInstance";
			break;
		case stop:
			chaosTypeName = "ShutdownInstance";
			break;
		default:
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		gen.writeStartObject();
		gen.writeArrayFieldStart("results");
		ChaosType chaosType = ChaosType.parse(monkey.getChaosTypes(), chaosTypeName);
		addTerminationEvent(BasicChaosCrawler.Types.TAG.name(), group, chaosType, gen);
		gen.writeEndArray();
		gen.writeEndObject();
		gen.close();
		LOGGER.info("entity content is '{}'", baos.toString("UTF-8"));

		return getInstances(group);
	}

	@POST
	@Path("/group/{group}/{instance}/{action}")
	public Response controlInstance(@PathParam("group") String group, @PathParam("instance") String instance,
			@PathParam("action") InstanceAction action) throws Exception {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);

		InstanceGroup groupInstance = new BasicInstanceGroup(group, BasicChaosCrawler.Types.TAG, null, null);

		if (!monkey.isGroupEnabled(groupInstance)) {

			gen.writeStartObject();
			gen.writeStringField("error", "group(" + group + ") is not enabled");
			gen.writeEndObject();

			return Response.status(Response.Status.BAD_REQUEST).entity(baos.toString("UTF-8")).build();
		}
		
		String chaosTypeName = null;
		switch (action) {
		case start:
			chaosTypeName = "StartInstance";
			break;
		case stop:
			chaosTypeName = "ShutdownInstance";
			break;
		default:
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		ChaosType chaosType = ChaosType.parse(monkey.getChaosTypes(), chaosTypeName);

		addTerminationEvent(BasicChaosCrawler.Types.TAG.name(), group, chaosType, instance, gen);

		gen.close();
		LOGGER.info("entity content is '{}'", baos.toString("UTF-8"));
		// return
		// Response.status(responseStatus).entity(baos.toString("UTF-8")).build();
		return getInstance(group, instance);
	}

	private Response.Status addTerminationEvent(String groupType, String groupName, ChaosType chaosType,
			String instance, JsonGenerator gen) throws IOException {
		LOGGER.info("Running on-demand termination for instance group type '{}' and name '{}'", groupType, groupName);
		Response.Status responseStatus;
		try {
			gen.writeStartObject();
			Event evt = monkey.terminateNow(groupType, groupName, chaosType, instance);
			if (evt != null) {
				responseStatus = Response.Status.OK;
				gen.writeStringField("instance", evt.id());
				gen.writeNumberField("createdAt", evt.eventTime().getTime());
				Map<String, String> fields = evt.fields();
				gen.writeStringField("group", fields.getOrDefault("groupName", ""));
				gen.writeStringField("type", fields.getOrDefault("chaosType", ""));
			} else {
				responseStatus = Response.Status.INTERNAL_SERVER_ERROR;
				gen.writeStringField("message",
						String.format("Failed to terminate instance in group %s [type %s]", groupName, groupType));
			}
		} catch (FeatureNotEnabledException e) {
			responseStatus = Response.Status.FORBIDDEN;
			gen.writeStringField("message", e.getMessage());
		} catch (InstanceGroupNotFoundException e) {
			responseStatus = Response.Status.NOT_FOUND;
			gen.writeStringField("message", e.getMessage());
		} catch (NotFoundException e) {
			// Available instance cannot be found to terminate, maybe the
			// instance is already gone
			responseStatus = Response.Status.GONE;
			gen.writeStringField("message", e.getMessage());
		} finally {
			gen.writeEndObject();
		}
		LOGGER.info("On-demand termination completed.");
		return responseStatus;
	}

	private Response.Status addTerminationEvent(String groupType, String groupName, ChaosType chaosType,
			JsonGenerator gen) throws IOException {
		LOGGER.info("Running on-demand termination for instance group type '{}' and name '{}'", groupType, groupName);
		for (InstanceGroup group : monkey.context().chaosCrawler().groups(groupName)) {
			if (monkey.isGroupEnabled(group)) {
				List<String> instances = new ArrayList<String>();
				instances.addAll(group.instances());
				for (String instance : instances) {
					System.out.println("terminate:"+instance+","+group);

					addTerminationEvent(groupType, groupName, chaosType, instance, gen);
				}
			}
		}

		LOGGER.info("On-demand termination completed.");
		return Response.Status.OK;
	}
}
