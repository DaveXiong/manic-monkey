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
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.manic.ManicChaosMonkey;
import com.netflix.simianarmy.resources.manic.hooker.HookerSearch.INDEX;
import com.netflix.simianarmy.resources.manic.hooker.ServiceLayerMock.Feature;
import com.sun.jersey.spi.resource.Singleton;

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
		gen.writeArrayFieldStart("types");
		for (HookerType type : HookerType.values()) {
			gen.writeString(type.toString());
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
	@Path("/{hookerType}")
	public Response getEvents(@PathParam("hookerType") String type) throws IOException {
		HookerType hookerType = HookerType.parse(type);
		List<Hooker<?, ?>> hookers = SL.getRecorder().getHookers(hookerType);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);
		gen.writeStartObject();
		gen.writeArrayFieldStart("results");
		for (Hooker<?, ?> hooker : hookers) {
			gen.writeObject(hooker);

		}
		gen.writeEndArray();
		gen.writeEndObject();
		gen.close();
		return Response.status(Response.Status.OK).entity(baos.toString("UTF-8")).build();
	}

	@PUT
	@Path("/{hookerType}")
	public Response addHooker(@PathParam("hookerType") String type, String data) throws Exception {
		System.out.println(data);

		HookerType hookerType = HookerType.parse(type);

		Map<INDEX, String> index = SL.getHookerSearch(hookerType).parseIndexes(data);

		SL.getRecorder().addHooker(hookerType, index.get(INDEX.MESSAGEID), index.get(INDEX.UUID),
				index.get(INDEX.COMMAND), data);

		return Response.status(Response.Status.OK).entity(data).build();

	}

	@DELETE
	@Path("/{hookerId}")
	public Response deleteHooker(@PathParam("hookerId") String id) throws Exception {
		System.out.println("Delete :" + id);

		SL.getRecorder().deleteHooker(id);

		return Response.status(Response.Status.OK).build();

	}

	enum Action {
		enable, disable
	}

	@POST
	@Path("/{feature}/{action}")
	public Response enable(@PathParam("feature") Feature feature, @PathParam("feature") Action action)
			throws Exception {
		if (action == Action.enable) {
			SL.enable(feature);
		} else {
			SL.disable(feature);
		}
		return Response.status(Response.Status.OK).build();

	}

}
