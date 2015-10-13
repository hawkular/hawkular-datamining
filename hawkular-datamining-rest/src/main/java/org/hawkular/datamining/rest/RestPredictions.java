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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.container.TimeoutHandler;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hawkular.dataminig.api.model.PredictionRequest;
import org.hawkular.dataminig.api.model.PredictionResult;
import org.hawkular.datamining.bus.listener.PredictionResultListener;
import org.hawkular.datamining.bus.sender.PredictionsRequestSender;

/**
 * @author Pavol Loffay
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestPredictions {

    private static final int REQUEST_TIMEOUT = 20;
    private static Double counter = 0.0;

    @Inject
    private PredictionsRequestSender predictionsRequestSender;

    @Inject
    private PredictionResultListener predictionResultListener;


    @GET
    @Path("/predictions/{metricId}")
    public Response predict(@Suspended final AsyncResponse asyncResponse,
                            @PathParam("metricId") String metricId,
                            @QueryParam("timestamp") List<Double> timestamps) {

        asyncResponse.setTimeoutHandler(new TimeoutHandler() {
            @Override
            public void handleTimeout(AsyncResponse asyncResponse) {
                asyncResponse.resume(Response.status(Response.Status.SERVICE_UNAVAILABLE).build());
            }
        });
        asyncResponse.setTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS);


        String predictionRequestId = String.valueOf(counter++);

        // send prediction requests to engine
        List<PredictionRequest> predictionRequests = timestamps.stream().map(timestamp -> {
            return new PredictionRequest(predictionRequestId, metricId, timestamp);
        }).collect(Collectors.toList());

        predictionsRequestSender.send(predictionRequests);

        // get predictions
        new Thread(new Runnable() {
            @Override
            public void run() {

                PredictionResult result;
                // todo get by requestID
                while ((result = predictionResultListener.cache.get(predictionRequestId)) == null) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                predictionResultListener.cache.remove(predictionRequestId);
                asyncResponse.resume(result);


            }
        }).start();


        return Response.ok().build();
    }
}
