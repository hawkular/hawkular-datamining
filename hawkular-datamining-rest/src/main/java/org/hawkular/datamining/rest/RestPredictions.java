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

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hawkular.datamining.api.Constants;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.forecast.DataPoint;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * @author Pavol Loffay
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "/models", description = "Learn and predict.", tags = "Models")
public class RestPredictions {

    @HeaderParam(Constants.TENANT_HEADER_NAME)
    private String tenant;

    @Inject
    private SubscriptionManager subscriptionManager;


    @GET
    @Path("/models/{metricId}/predict")
    @ApiOperation("Predict future values of given metric.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success, Predictions returned"),
            @ApiResponse(code = 404, message = "Prediction of given metric not found", response = ApiError.class),
            @ApiResponse(code = 500, message = "Server error",response = ApiError.class)
    })
    public Response predict(@PathParam("metricId") String metricId,
                            @DefaultValue("1") @QueryParam("ahead") int ahead) {

        List<DataPoint> dataPoints = subscriptionManager.subscription(tenant, metricId)
                .forecaster().forecast(ahead);

        return Response.ok().entity(dataPoints).build();
    }

    @POST
    @Path("/models/{metricId}/learn")
    @ApiOperation("Learn model based on inserted values.")
    @ApiResponses(value = {
            @ApiResponse(code = 201, message = "Success, Learning successfully processed"),
            @ApiResponse(code = 400, message = "Missing or invalid payload", response = ApiError.class),
            @ApiResponse(code = 404, message = "Prediction of given metric not found", response = ApiError.class),
            @ApiResponse(code = 500, message = "Server error",response = ApiError.class)
    })
    public Response learn(@PathParam("metricId") String metricId, List<DataPoint> data) {

        subscriptionManager.subscription(tenant, metricId).forecaster().learn(data);

        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
