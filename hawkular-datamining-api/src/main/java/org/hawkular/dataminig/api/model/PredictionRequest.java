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

package org.hawkular.dataminig.api.model;

import java.io.Serializable;

/**
 * @author Pavol Loffay
 */
public class PredictionRequest implements Serializable {

    private String requestId;

    private String metricId;
    private Double timestamp;


    public PredictionRequest() {
    }

    public PredictionRequest(String requestId, String metricId, Double timestamp) {
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp shouldn't be null");
        }

        this.requestId = requestId;
        this.metricId = metricId;
        this.timestamp = timestamp;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMetricId() {
        return metricId;
    }

    public void setMetricId(String metricId) {
        this.metricId = metricId;
    }

    public Double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Double timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Prediction data [metricId=" + metricId + ", features=" + timestamp.toString() + "]";
    }
}
