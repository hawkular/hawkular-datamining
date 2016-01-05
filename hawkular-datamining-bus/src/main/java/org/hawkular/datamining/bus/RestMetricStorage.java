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

package org.hawkular.datamining.bus;

import java.util.Collections;
import java.util.List;

import org.hawkular.datamining.api.Constants;
import org.hawkular.datamining.api.model.BucketPoint;
import org.hawkular.datamining.api.model.DataPoint;
import org.hawkular.datamining.api.storage.MetricStorage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

/**
 * @author Pavol Loffay
 */
public class RestMetricStorage implements MetricStorage {

    // todo
    private  String BASE_URL = "http://localhost:8080/hawkular/metrics";

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    public RestMetricStorage() {
        this.okHttpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public List<DataPoint> loadPoints(String metricId, String tenant) {

        String url = BASE_URL + "/gauges/" + UrlUtils.encodeUrlPath(metricId) + "/data?start=1";

        Request request = UrlUtils.buildJsonRequest(url,
                Collections.singletonMap(Constants.TENANT_HEADER_NAME, tenant));

        List<DataPoint> result = UrlUtils.execute(request,
                new TypeReference<List<DataPoint>>() {},
                okHttpClient,
                objectMapper);

        return result;
    }

    public List<BucketPoint> loadBuckets(long buckets, String metricId, String tenant) {

        String url = BASE_URL + "/gauges/"  + UrlUtils.encodeUrlPath(metricId) + "/data?start=1&buckets=" + buckets;

        Request request = UrlUtils.buildJsonRequest(url,
                Collections.singletonMap(Constants.TENANT_HEADER_NAME, tenant));

        List<BucketPoint> result = UrlUtils.execute(request,
                new TypeReference<List<BucketPoint>>() {},
                okHttpClient,
                objectMapper);

        return result;
    }
}
