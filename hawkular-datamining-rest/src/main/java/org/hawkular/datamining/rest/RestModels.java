/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
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
import org.hawkular.datamining.api.DataMiningEngine;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.api.model.MetricData;
import org.hawkular.datamining.cdi.qualifiers.Official;

/**
 * @author Pavol Loffay
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestModels {

    @Inject
    private SubscriptionManager subscriptionManager;

    @Official
    @Inject
    private DataMiningEngine<MetricData> dataMiningEngine = null;

    @HeaderParam(Constants.TENANT_HEADER_NAME)
    private String tenant;


    @GET
    @Path("/models")
    public Response getAll() {
        Set<Metric> tenantsSubscriptions = subscriptionManager.metricsOfTenant(tenant);

        return Response.status(Response.Status.OK).entity(tenantsSubscriptions).build();
    }

    @GET
    @Path("/models/{metricId}")
    public Response getOne(@PathParam("metricId") String metricId) {
        Metric metric = subscriptionManager.metric(tenant, metricId);

        return Response.status(Response.Status.OK).entity(metric).build();
    }

    @POST
    @Path("/models")
    public Response subscribe(Metric.RestBlueprint blueprint) {

        Metric metric = new Metric(blueprint, tenant, null);
        subscriptionManager.subscribe(metric,
                new HashSet<>(Arrays.asList(SubscriptionManager.ModelOwner.Metric)));

        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/models/{id}")
    public Response unSubscribe(@PathParam("id") String metricId) {

        subscriptionManager.unSubscribe(tenant, metricId);

        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
