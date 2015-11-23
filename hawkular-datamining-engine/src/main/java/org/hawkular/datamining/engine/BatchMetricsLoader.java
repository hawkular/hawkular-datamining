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

package org.hawkular.datamining.engine;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.hawkular.datamining.api.Constants;
import org.hawkular.datamining.api.model.BucketPoint;
import org.hawkular.datamining.api.model.DataPoint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

/**
 * @author Pavol Loffay
 */
public class BatchMetricsLoader {

    private  String BASE_URL;

    private final String metricsId;
    private final String tenant;

    private final OkHttpClient okHttpClient;


    public BatchMetricsLoader(String tenant, String metricsId) {
        this.metricsId = metricsId;
        this.tenant = tenant;

        try {
            EngineConfiguration engineConfiguration = new EngineConfiguration();
            BASE_URL = engineConfiguration.getProperty("hawkular.metrics.address");
        } catch (IOException e) {
            // todo
        }

        this.okHttpClient = new OkHttpClient();
    }

    public List<DataPoint> loadPoints() {

        List<DataPoint> result = null;
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/gauges/" +
                            URLEncoder.encode(metricsId, "UTF-8").replace("+", "%20") +
                            "/data?start=1")
                    .header(Constants.TENANT_HEADER_NAME, tenant)
                    .get()
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                return result;
            }

            ResponseBody responseBody = response.body();

            // unmarshall
            ObjectMapper objectMapper = new ObjectMapper();
            result = objectMapper.readValue(responseBody.bytes(), new TypeReference<List<DataPoint>>() {});

        } catch (IOException ex) {
            EngineLogger.LOGGER.batchLoadingfailed(ex.getMessage());
        }

        return result;
    }

    public List<BucketPoint> loadBuckets(long buckets) {

        List<BucketPoint> result = null;
        try {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/gauges/"
                            + URLEncoder.encode(metricsId, "UTF-8").replace("+", "%20") +
                            "/data?start=1&buckets=" + buckets)
                    .header(Constants.TENANT_HEADER_NAME, tenant)
                    .get()
                    .build();

            Response response = okHttpClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                return result;
            }

            ResponseBody responseBody = response.body();

            // unmarshall
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            result = objectMapper.readValue(responseBody.bytes(), new TypeReference<List<BucketPoint>>() {});

        } catch (IOException ex) {
            EngineLogger.LOGGER.batchLoadingfailed(ex.getMessage());
        }

        return result;
    }
}
