/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hawkular.datamining.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hawkular.datamining.engine.MetricFilter;

/**
 * @author Pavol Loffay
 */
@Path("/metrics")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestMetrics {

    @GET
    @Path("/subscribe/{key}")
    public Response contains(@PathParam("key") String key) {

        Response.Status status = MetricFilter.contains(key) ? Response.Status.OK : Response.Status.NOT_FOUND;

        return Response.status(status).build();
    }

    @POST
    @Path("/subscribe")
    public Response subscribe(String key) {

        if (null != key) {
            MetricFilter.subscribe(key);
        }

        return Response.status(Response.Status.OK).build();
    }

    @DELETE
    @Path("/subscribe/{key}")
    public Response unSubscribe(@PathParam("key") String key) {

        if (null != key) {
            MetricFilter.unSubscribe(key);
        }

        return Response.status(Response.Status.OK).build();
    }
}
