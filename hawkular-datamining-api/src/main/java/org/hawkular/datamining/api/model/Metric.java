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

import org.hawkular.datamining.forecast.MetricContext;

/**
 * @author Pavol Loffay
 */
public class Metric implements MetricContext {

    private final String id;
    private final String feed;
    private final String tenant;

    private Long collectionInterval;
    private Long forecastingHorizon;

    private MetricType metricType;


    public Metric(String tenant, String feed, String id, Long collectionInterval, Long forecastingHorizon,
                  MetricType metricType) {

        this.tenant = tenant;
        this.feed = feed;
        this.id = id;
        this.collectionInterval = collectionInterval;
        this.forecastingHorizon = forecastingHorizon;
        this.metricType = metricType;
    }

    public Metric(Metric that) {
        this.tenant = that.getTenant();
        this.feed = that.getFeed();
        this.id = that.getMetricId();
        this.collectionInterval = that.getCollectionInterval();
        this.forecastingHorizon = that.forecastingHorizon;
        this.metricType = that.metricType;
    }

    public Metric(RestBlueprint restBlueprint, String tenant, String feed) {
        this.collectionInterval = restBlueprint.getCollectionInterval();
        this.id = restBlueprint.getMetricId();
        this.tenant = tenant;
        this.feed = feed;
    }

    public void setCollectionInterval(Long collectionInterval) {
        this.collectionInterval = collectionInterval;
    }

    @Override
    public String getTenant() {
        return tenant;
    }
    @Override
    public String getMetricId() {
        return id;
    }

    @Override
    public Long getCollectionInterval() {
        if (this.collectionInterval != null) {
            return collectionInterval;
        }

        if (metricType != null && metricType.getCollectionInterval() != null) {
            return metricType.getCollectionInterval();
        }

        return null;
    }

    public String getFeed() {
        return feed;
    }

    public MetricType getMetricType() {
        return metricType;
    }

    /**
     * @return prediction interval in seconds
     */
    public Long getForecastingHorizon() {
        return forecastingHorizon;
    }

    /**
     * @param forecastingHorizon in seconds
     */
    public void setForecastingHorizon(Long forecastingHorizon) {
        this.forecastingHorizon = forecastingHorizon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Metric)) return false;

        Metric metric = (Metric) o;

        if (!tenant.equals(metric.tenant)) return false;
        return id.equals(metric.id);

    }

    @Override
    public int hashCode() {
        int result = tenant.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }

    public static class RestBlueprint {
        private String metricId;
        private Long collectionInterval;

        public RestBlueprint() {
        }

        public RestBlueprint(String metricId, Long collectionInterval) {
            this.metricId = metricId;
            this.collectionInterval = collectionInterval;
        }

        public String getMetricId() {
            return metricId;
        }

        public Long getCollectionInterval() {
            return collectionInterval;
        }
    }

    public static class Update {
        private Long collectionInterval;
        private Long forecastingHorizon;


        private Update() {
        }

        public Update(Long collectionInterval, Long forecastingHorizon) {
            this.collectionInterval = collectionInterval;
            this.forecastingHorizon = forecastingHorizon;
        }

        public Long getCollectionInterval() {
            return collectionInterval;
        }

        public Long getForecastingHorizon() {
            return forecastingHorizon;
        }
    }
}
