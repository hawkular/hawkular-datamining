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

package org.hawkular.datamining.inventory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.inventory.api.Inventory;
import org.hawkular.inventory.api.Query;
import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.filters.Related;
import org.hawkular.inventory.api.filters.RelationWith;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.CanonicalPath;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.Relationship;
import org.hawkular.inventory.api.paging.Order;
import org.hawkular.inventory.api.paging.Page;
import org.hawkular.inventory.api.paging.Pager;
import org.hawkular.inventory.base.spi.SwitchElementType;

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

        InventoryLogger.LOGGER.inventoryInitialized(predictedMetrics.size());
    }

    @Override
    public Set<Relationship> predictionRelationships(CanonicalPath... targetEntity) {

        Query query = Query.path().with(With.paths(targetEntity)).with(SwitchElementType.incomingRelationships(),
                        RelationWith.name(InventoryConfiguration.PREDICTION_RELATIONSHIP)).get();

        Page<Relationship> page = inventory.execute(query, Relationship.class, Pager.unlimited(Order.unspecified()));

        return new HashSet<>(page.toList());
    }

    @Override
    public Metric metric(CanonicalPath metric) {
        Query query = Query.path().with(With.path(metric), With.type(Metric.class)).get();

        Page<Metric> page = inventory.execute(query, Metric.class, Pager.unlimited(Order.unspecified()));

        List<Metric> metrics = page.toList();
        return metrics.size() > 0 ? metrics.get(0) : null;
    }

    @Override
    public Set<Metric> metricsOfType(CanonicalPath metricType) {
        Query query = Query.path().with(With.path(metricType))
                .with(Related.by(Relationships.WellKnown.defines))
                .with(With.type(Metric.class)).get();

        Page<Metric> page = inventory.execute(query, Metric.class, Pager.unlimited(Order.unspecified()));

        List<Metric> metrics = page.toList();
        return new HashSet<>(metrics);
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
