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

package org.hawkular.datamining.dist.integration.metrics;

import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Specializes;

import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.api.model.MetricDataType;
import org.hawkular.datamining.cdi.EmptyMetricsClient;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.metrics.core.service.MetricsService;
import org.hawkular.metrics.core.service.Order;
import org.hawkular.metrics.model.MetricId;
import org.hawkular.metrics.model.MetricType;

/**
 * @author Pavol Loffay
 */
@Specializes
@ApplicationScoped
public class CDIMetricsClient extends EmptyMetricsClient {

    @javax.annotation.Resource(lookup = "java:global/Hawkular/Metrics")
    private MetricsService metricsService;

    @Override
    public List<DataPoint> loadPoints(Metric metric, long start, long end) {

        MetricType type = metric.getMetricType().getMetricDataType() == MetricDataType.COUNTER ?
                MetricType.COUNTER :
                MetricType.GAUGE;

        MetricId<Double> metricId = new MetricId<>(metric.getTenant(), type, metric.getMetricId());

        List<org.hawkular.metrics.model.DataPoint<Double>> first =
                metricsService.findDataPoints(metricId, start, end, Integer.MAX_VALUE, Order.ASC)
                        .toList().toBlocking().first();

        return convertDataPoints(first);
    }

    private static List<DataPoint> convertDataPoints(List<org.hawkular.metrics.model.DataPoint<Double>> dataPoints) {

       return dataPoints.stream().map(dataPoint -> new DataPoint(dataPoint.getValue(), dataPoint.getTimestamp()))
                        .collect(Collectors.toList());
    }

    private static List<DataPoint> convertBuckets(List<org.hawkular.metrics.model.NumericBucketPoint> buckets) {

        return buckets.stream().map(bucket -> new DataPoint(bucket.getAvg(), bucket.getEnd()))
                .collect(Collectors.toList());
    }
}
