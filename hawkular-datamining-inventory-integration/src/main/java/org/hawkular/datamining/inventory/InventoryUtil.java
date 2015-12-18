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

package org.hawkular.datamining.inventory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawkular.inventory.api.model.CanonicalPath;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.Relationship;

/**
 * @author Pavol Loffay
 */
public class InventoryUtil {

    public static Long parsePredictionInterval(Map<String, Object> properties) {
        Long predictionInterval = Long.parseLong(
                (String) properties.get(InventoryConfiguration.PREDICTION_INTERVAL_PROP));
        return predictionInterval;
    }

    public static Set<org.hawkular.datamining.api.model.Metric> convertMetrics(Set<Metric> metrics,
                                             Set<Relationship> relationships) {

        Set<org.hawkular.datamining.api.model.Metric> result = new HashSet<>(metrics.size());

        for (Metric invMetric: metrics) {
            org.hawkular.datamining.api.model.Metric metric = convertMetric(invMetric, relationships);
            result.add(metric);
        }

        return result;
    }

    public static org.hawkular.datamining.api.model.Metric convertMetric(Metric invMetric,
                                                                         Set<Relationship> relationships) {
        Long metricPredictionInterval = predictionInterval(relationships, invMetric.getPath());
        Long typePredictionInterval = predictionInterval(relationships, invMetric.getType().getPath());

        org.hawkular.datamining.api.model.MetricType type = new org.hawkular.datamining.api.model.MetricType(
                invMetric.getType().getCollectionInterval(), typePredictionInterval);

        String tenant = invMetric.getPath().ids().getTenantId();
        String feed = invMetric.getPath().ids().getFeedId();
        org.hawkular.datamining.api.model.Metric metric = new org.hawkular.datamining.api.model.Metric(tenant,
                feed, invMetric.getId(), invMetric.getCollectionInterval(), type, metricPredictionInterval);

        return metric;
    }

    public static org.hawkular.datamining.api.model.Metric convertMetric(Metric invMetric,
                                                                         Long typePredictionInterval,
                                                                         Long metricPredictionInterval) {

        org.hawkular.datamining.api.model.MetricType type = new org.hawkular.datamining.api.model.MetricType(
                invMetric.getType().getCollectionInterval(), typePredictionInterval);

        String tenant = invMetric.getPath().ids().getTenantId();
        String feed = invMetric.getPath().ids().getFeedId();
        org.hawkular.datamining.api.model.Metric metric = new org.hawkular.datamining.api.model.Metric(tenant,
                feed, invMetric.getId(), invMetric.getCollectionInterval(), type, metricPredictionInterval);

        return metric;
    }

    public static Long predictionInterval(Set<Relationship> relationships, CanonicalPath targetEntityPath) {

        Long predictionInterval = null;
        for (Relationship relationship: relationships) {

            if (relationship.getTarget().equals(targetEntityPath)) {
                predictionInterval = Long.parseLong((String)relationship.getProperties().get("predictionInterval"));
            }
        }

        return predictionInterval;
    }
}
