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
package com.netflix.simianarmy.resources;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;

import com.sun.jersey.spi.resource.Singleton;

/**
 * The Class HeartBeatResource for json REST apis.
 */
@Path("/v1/heartbeat")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class HeartBeatResource {

    /** The Constant JSON_FACTORY. */
    private static final MappingJsonFactory JSON_FACTORY = new MappingJsonFactory();

    /**
     * Instantiates a chaos monkey resource using a registered chaos monkey from factory.
     */
    public HeartBeatResource() {

    }

    /**
     * Gets the chaos heartbeat. Creates GET /api/v1/heartbeat 
     * 
     * @param uriInfo
     *            the uri info
     * @return the chaos events json response
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @GET
    public Response getHeartBeat() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);

        gen.writeStartObject();
        gen.writeStringField("version", "2.0.0");
        gen.writeNumberField("timestamp", System.currentTimeMillis());

        gen.writeEndObject();

        gen.close();
        return Response.status(Response.Status.OK).entity(baos.toString("UTF-8")).build();
    }
}
