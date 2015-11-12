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

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hawkular.dataminig.api.Constants;
import org.hawkular.dataminig.api.Official;
import org.hawkular.dataminig.api.model.DataPoint;
import org.hawkular.datamining.engine.model.ForecastingEngine;

/**
 * @author Pavol Loffay
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestPredictions {

    @HeaderParam(Constants.TENANT_HEADER_NAME)
    private String tenant;

    @Inject
    @Official
    private ForecastingEngine forecastingEngine;


    @GET
    @Path("/predict/{metricId}")
    public Response predict(@PathParam("metricId") String metricId,
                            @DefaultValue("1") @QueryParam("ahead") int ahead) {

        List<DataPoint> dataPoints = forecastingEngine.predict(tenant, metricId, ahead);

        return Response.ok().entity(dataPoints).build();
    }
}
