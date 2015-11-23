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

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hawkular.datamining.api.Constants;
import org.hawkular.datamining.api.MetricFilter;

/**
 * @author Pavol Loffay
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestMetrics {

    @HeaderParam(Constants.TENANT_HEADER_NAME)
    private String tenant;

    @GET
    @Path("/metrics")
    public Response getAll() {
        Set<String> tenantsSubscriptions = MetricFilter.getTenantSubscription(tenant);
        return Response.status(Response.Status.OK).entity(tenantsSubscriptions).build();
    }

    @POST
    @Path("/metrics/{id}")
    public Response subscribe(@PathParam("id") String metricsId) {

        if (null != metricsId) {
            MetricFilter.subscribe(tenant, metricsId);
        }

        return Response.status(Response.Status.OK).build();
    }

    @DELETE
    @Path("/metrics/{id}")
    public Response unSubscribe(@PathParam("id") String metricsId) {

        if (null != metricsId && !metricsId.isEmpty()) {
            MetricFilter.unSubscribe(tenant, metricsId);
        }

        return Response.status(Response.Status.OK).build();
    }
}
