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

package org.hawkular.datamining.forecast;

/**
 * @author Pavol Loffay
 */
public class ImmutableMetricContext implements MetricContext {

    private Long collectionInterval;
    private String metricId;
    private String tenant;


    /**
     * @param tenant id of tenant
     * @param metricId id of metric
     * @param collectionInterval collection interval of metric in seconds
     */
    public ImmutableMetricContext(String tenant, String metricId, Long collectionInterval) {
        if (collectionInterval < 1) {
            throw new IllegalArgumentException("Collection interval should be at least 1");
        }

        this.tenant = tenant;
        this.metricId = metricId;
        this.collectionInterval = collectionInterval;
    }

    public static ImmutableMetricContext getDefault() {
        return new ImmutableMetricContext(null, null, 1L);
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    @Override
    public String getMetricId() {
        return metricId;
    }

    @Override
    public Long getCollectionInterval() {
        return collectionInterval;
    }

    @Override
    public void setCollectionInterval(Long collectionInterval) {
        this.collectionInterval = collectionInterval;
    }

    @Override
    public String toString() {
        return "ImmutableMetricContext{" +
                "collectionInterval=" + collectionInterval +
                ", metricId='" + metricId + '\'' +
                '}';
    }
}
