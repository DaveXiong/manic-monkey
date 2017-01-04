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
package com.netflix.simianarmy.resources.manic.hooker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.manic.ManicChaosMonkey;
import com.sun.jersey.spi.resource.Singleton;

import console.mw.sl.service.schema.AllocatePortPayload;

/**
 * The Class ManicMonkeyResource for json REST apis.
 */
@Path("/v1/manic/hooker")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class ManicSLResource {

	/** The Constant JSON_FACTORY. */
	private static final MappingJsonFactory JSON_FACTORY = new MappingJsonFactory();

	/** The monkey. */
	private ManicChaosMonkey monkey = null;

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ManicSLResource.class);

	public static final ServiceLayerMock SL = new ServiceLayerMock();

	/**
	 * Instantiates a chaos monkey resource using a registered chaos monkey from
	 * factory.
	 */
	public ManicSLResource() {
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
		gen.writeBooleanField("ping", true);

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
		for (Map.Entry<String, List<String>> pair : uriInfo.getQueryParameters().entrySet()) {
			if (pair.getValue().isEmpty()) {
				continue;
			}
			query.put(pair.getKey(), pair.getValue().get(0));

		}

		List<PortHooker> hookers = SL.getRecorder().getPortHookers(query);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);
		gen.writeStartObject();
		gen.writeArrayFieldStart("results");
		for (PortHooker hooker : hookers) {
			gen.writeObject(hooker);

		}
		gen.writeEndArray();
		gen.writeEndObject();
		gen.close();
		return Response.status(Response.Status.OK).entity(baos.toString("UTF-8")).build();
	}

	@POST
	@Path("/{hookerType}")
	public Response addHooker(@PathParam("hookerType") String type, String data) throws Exception {
		System.out.println(data);

		switch (type.toUpperCase()) {
		case "PORT":
			Type portHookerType = new TypeToken<Hooker<PortCommandArgs, AllocatePortPayload>>() {
			}.getType();
			Hooker<PortCommandArgs, AllocatePortPayload> portHooker = new Gson().fromJson(data, portHookerType);
		case "CONNECTION":
		case "CR":
		case "PEER":
		case "AWS":
		default:
			break;
		}

		PortHooker hooker = new Gson().fromJson(data, PortHooker.class);

		SL.getRecorder().addHooker(hooker);

		return Response.status(Response.Status.OK).entity(data).build();

	}

	@DELETE
	@Path("/port/{id}")
	public Response deleteHooker(@PathParam("id") String id) throws Exception {
		System.out.println("Delete :" + id);

		SL.getRecorder().deleteHooker(id);

		return Response.status(Response.Status.OK).build();

	}

	@POST
	@Path("/port/enable")
	public Response enablePortMock() throws Exception {
		SL.enablePortMock();
		return Response.status(Response.Status.OK).build();

	}

	@POST
	@Path("/port/disable")
	public Response disablePortMock() throws Exception {
		SL.disablePortMock();
		return Response.status(Response.Status.OK).build();

	}

	@POST
	@Path("/port/{uuid}")
	public Response addCommand(@PathParam("uuid") String uuid, String data) throws Exception {
		System.out.println(uuid + ":" + data);
		return Response.status(Response.Status.OK).entity(data).build();

	}

}
