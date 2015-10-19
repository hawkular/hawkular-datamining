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

package org.hawkular.datamining.engine.receiver;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

import org.hawkular.dataminig.api.model.MetricData;
import org.hawkular.dataminig.api.model.TimeSeries;
import org.hawkular.datamining.engine.EngineLogger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

/**
 * @author Pavol Loffay
 */
public class BatchMetricsLoader {

    private static final String BASE_URL = "http://localhost:8080/hawkular/metrics/gauges/";

    private final String metricId;
    private final OkHttpClient okHttpClient;


    public BatchMetricsLoader(String metricId) {
        this.metricId = metricId;

        this.okHttpClient = new OkHttpClient();
    }

    public List<MetricData> load() {

        List<MetricData> result = null;
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL  + URLEncoder.encode(metricId, "UTF-8").replace("+", "%20") + "/data?start=1")
                    .header("Hawkular-Tenant", "28026b36-8fe4-4332-84c8-524e173a68bf")
                    .get()
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            ResponseBody responseBody = response.body();

            // unmarshall
            ObjectMapper objectMapper = new ObjectMapper();
            List<TimeSeries> timeSeries = objectMapper.readValue(responseBody.bytes(), new
                    TypeReference<List<TimeSeries>>() {});

            result = timeSeries.stream().map(x -> new MetricData(metricId, x.getTimestamp(), x.getValue()))
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            EngineLogger.LOGGER.batchLoadingfailed(ex.getMessage());
        }

        return result;
    }
}
