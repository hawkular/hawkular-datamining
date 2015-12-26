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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.inventory.api.Inventory;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.CanonicalPath;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.Relationship;

/**
 * @author Pavol Loffay
 */
@ApplicationScoped
public class InventoryCDIStorage implements InventoryStorage {

    @javax.annotation.Resource(lookup = "java:global/Hawkular/Inventory")
    private Inventory inventory;

    @Inject
    private SubscriptionManager subscriptionManager;

    @PostConstruct
    public void init() {
        Set<org.hawkular.datamining.api.model.Metric> predictedMetrics = getAllPredictedMetrics();
        predictedMetrics.forEach(metric -> subscriptionManager.subscribe(metric));
    }

    // TODO optimize
    @Override
    public Set<Relationship> predictionRelationships(CanonicalPath... targetEntity) {
        Set<Relationship> relationships =
                inventory.relationships().named(InventoryConfiguration.PREDICTION_RELATIONSHIP).entities();

        Set<CanonicalPath> targetPaths = new HashSet<>(Arrays.asList(targetEntity));
        Set<Relationship> result =
                relationships.stream().filter(relationship -> targetPaths.contains(relationship.getTarget())).collect(
                        Collectors.toSet());

        return result;
    }

    @Override
    public Metric metric(CanonicalPath metric) {
        Set<Metric> metrics =
                inventory.tenants().getAll().feeds().getAll().metrics().getAll(With.path(metric)).entities();

        return metrics.size() > 0 ? metrics.iterator().next() : null;
    }

    @Override
    public Set<Metric> metricsOfType(CanonicalPath metricType) {
        return inventory.tenants().getAll().feeds().getAll().metricTypes().getAll(With.path(metricType))
                .metrics().getAll().entities();
    }

    private Set<org.hawkular.datamining.api.model.Metric> getAllPredictedMetrics() {
        Set<Relationship> relationships =
                inventory.relationships().named(InventoryConfiguration.PREDICTION_RELATIONSHIP).entities();

        Set<CanonicalPath> metricsCp = new HashSet<>();
        Set<CanonicalPath> metricTypesCp = new HashSet<>();

        for (Relationship relationship: relationships) {
            if (relationship.getTarget().getSegment().getElementType().equals(Metric.class)) {
                metricsCp.add(relationship.getTarget());
            } else if (relationship.getTarget().getSegment().getElementType().equals(MetricType.class)) {
                metricTypesCp.add(relationship.getTarget());
            }
        }

        Set<Metric> metrics = inventory.tenants().getAll().feeds().getAll().metrics()
                .getAll(With.paths(metricsCp.toArray(new CanonicalPath[0]))).entities();
        Set<Metric> metricsUnderTypes = inventory.tenants().getAll().feeds().getAll().metricTypes()
                .getAll(With.paths(metricTypesCp.toArray(new CanonicalPath[0]))).metrics().getAll().entities();

        metrics.addAll(metricsUnderTypes);

        return InventoryUtil.convertMetrics(metrics, relationships);
    }
}
