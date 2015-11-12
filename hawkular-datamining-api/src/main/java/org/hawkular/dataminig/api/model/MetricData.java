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
public class MetricData implements Serializable {

    private String tenant;
    private String metricId;
    private DataPoint dataPoint;

    public MetricData(String tenant, String metricId, Long timestamp, Double value) {
        this.tenant = tenant;
        this.metricId = metricId;
        this.dataPoint = new DataPoint(value, timestamp);
    }

    public String getMetricId() {
        return metricId;
    }

    public String getTenant() {
        return tenant;
    }

    public DataPoint getDataPoint() {
        return dataPoint;
    }
}
