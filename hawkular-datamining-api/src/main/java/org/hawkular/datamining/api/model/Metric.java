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

package org.hawkular.datamining.api.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Pavol Loffay
 */
public class Metric {

    private static final Pattern resourcePattern = Pattern.compile("\\~\\[([a-zA-Z0-9~-]+)\\]\\~");

    private String tenant;
    private String id;
    private Long interval;

    public Metric(String tenant, String id, Long interval) {
        this.tenant = tenant;
        this.id = id;
        this.interval = interval;
    }

    public Metric(Metric that) {
        this.tenant = that.getTenant();
        this.id = that.getId();
        this.interval = that.interval;
    }

    public Metric(RestBlueprint restBlueprint, String tenant) {
        this.interval = restBlueprint.getInterval();
        this.id = restBlueprint.getMetricId();
        this.tenant = tenant;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public String getTenant() {
        return tenant;
    }

    public String getFeed() {

        return getFeed(id);
    }

    public String getId() {
        return id;
    }

    public Long getInterval() {
        return interval;
    }

    public static String getFeed(String metricId) {
        Matcher matcher = resourcePattern.matcher(metricId);

        String feedId = null;
        if (matcher.find()) {
            feedId = matcher.group(1);
        } else {
            // todo throw ex
        }

        feedId = feedId.substring(0, feedId.indexOf("~"));
        return feedId;
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
        private Long interval;

        public RestBlueprint(String metricId, Long interval) {
            this.metricId = metricId;
            this.interval = interval;
        }

        public String getMetricId() {
            return metricId;
        }

        public Long getInterval() {
            return interval;
        }
    }
}
