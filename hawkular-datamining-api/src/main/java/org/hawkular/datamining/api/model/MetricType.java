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
package org.hawkular.datamining.api.model;

/**
 * @author Pavol Loffay
 */
public class MetricType {

    private String path;
    private Long collectionInterval;

    private MetricDataType metricDataType;

    public MetricType() {
    }

    public MetricType(String id, Long collectionInterval, MetricDataType metricDataType) {
        this.path = id;
        this.collectionInterval = collectionInterval;
        this.metricDataType = metricDataType;
    }

    public String getPath() {
        return path;
    }

    public Long getCollectionInterval() {
        return collectionInterval;
    }

    public void setCollectionInterval(Long collectionInterval) {
        this.collectionInterval = collectionInterval;
    }

    public MetricDataType getMetricDataType() {
        return metricDataType;
    }
}
