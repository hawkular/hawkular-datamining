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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavol Loffay
 */
public class PredictionResult {

    private String requestId;
    private String metricId;
    private List<DataPoint> points = new ArrayList<>();


    public PredictionResult() {
    }

    public PredictionResult(String requestId, String metricId, List<DataPoint> points) {
        this.requestId = requestId;
        this.metricId = metricId;
        this.points = points;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void addPoint(DataPoint dataPoint) {
        points.add(dataPoint);
    }

    public String getMetricId() {
        return metricId;
    }

    public void setMetricId(String metricId) {
        this.metricId = metricId;
    }

    public List<DataPoint> getPoints() {
        return points;
    }

    public void setPoints(List<DataPoint> points) {
        this.points = points;
    }
}
