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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.util.Eager;
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
import org.hawkular.inventory.api.model.Tenant;
import org.hawkular.inventory.api.paging.Order;
import org.hawkular.inventory.api.paging.Page;
import org.hawkular.inventory.api.paging.Pager;
import org.hawkular.inventory.base.spi.SwitchElementType;

/**
 * @author Pavol Loffay
 */
@Eager
@ApplicationScoped
public class InventoryCDIStorage implements InventoryStorage {

    @javax.annotation.Resource(lookup = "java:global/Hawkular/Inventory")
    private Inventory inventory;

    @Inject
    private SubscriptionManager subscriptionManager;

    @PostConstruct
    public void init() {
        Map<org.hawkular.datamining.api.model.Metric, Set<SubscriptionManager.SubscriptionOwner>> allPredictedMetrics =
                getAllPredictedMetrics();

        for (Map.Entry<org.hawkular.datamining.api.model.Metric, Set<SubscriptionManager.SubscriptionOwner>> entry:
                allPredictedMetrics.entrySet()) {

            subscriptionManager.subscribe(entry.getKey(), entry.getValue());
        }

        InventoryLogger.LOGGER.inventoryInitialized(allPredictedMetrics.size());
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

    @Override
    public Set<Metric> metricsUnderTenant(CanonicalPath tenant) {
        Set<Metric> entities = inventory.tenants().get(tenant.ids().getTenantId())
                .feeds().getAll().metrics().getAll().entities();

        return entities;
    }

    private Map<org.hawkular.datamining.api.model.Metric, Set<SubscriptionManager.SubscriptionOwner>>
    getAllPredictedMetrics() {

        Set<Relationship> relationships =
                inventory.relationships().named(InventoryConfiguration.PREDICTION_RELATIONSHIP).entities();

        Set<CanonicalPath> metricsCp = new HashSet<>();
        Set<CanonicalPath> metricTypesCp = new HashSet<>();
        Set<CanonicalPath> tenantsCp = new HashSet<>();

        for (Relationship relationship: relationships) {
            if (relationship.getTarget().getSegment().getElementType().equals(Metric.class)) {
                metricsCp.add(relationship.getTarget());
            } else if (relationship.getTarget().getSegment().getElementType().equals(MetricType.class)) {
                metricTypesCp.add(relationship.getTarget());
            } else if (relationship.getTarget().getSegment().getElementType().equals(Tenant.class)) {
                tenantsCp.add(relationship.getTarget());
            }
        }

        Set<Metric> metrics = inventory.tenants().getAll().feeds().getAll().metrics()
                .getAll(With.paths(metricsCp.toArray(new CanonicalPath[0]))).entities();
        Set<Metric> metricsUnderTypes = inventory.tenants().getAll().feeds().getAll().metricTypes()
                .getAll(With.paths(metricTypesCp.toArray(new CanonicalPath[0]))).metrics().getAll().entities();
        Set<Metric> metricsUnderTenant = inventory.tenants().getAll(With.paths(tenantsCp.toArray(new CanonicalPath[0])))
                        .feeds().getAll().metrics().getAll().entities();

        metrics.addAll(metricsUnderTypes);
        metrics.addAll(metricsUnderTenant);


        Set<org.hawkular.datamining.api.model.Metric> dataminingMetrics =
                InventoryUtil.convertMetrics(metrics, relationships);

        return metricsWithOwnersOfSubscription(relationships, dataminingMetrics);
    }

    private Map<org.hawkular.datamining.api.model.Metric, Set<SubscriptionManager.SubscriptionOwner>>
    metricsWithOwnersOfSubscription(Set<Relationship> relationships,
                                    Set<org.hawkular.datamining.api.model.Metric> dataminingMetrics) {

        Map<org.hawkular.datamining.api.model.Metric, Set<SubscriptionManager.SubscriptionOwner>> result =
                new HashMap<>();

        Map<String, SubscriptionManager.SubscriptionOwner> metrics = toIdAndOwner(relationships,
                SubscriptionManager.SubscriptionOwner.Metric);
        Map<String, SubscriptionManager.SubscriptionOwner> metricTypes = toIdAndOwner(relationships,
                SubscriptionManager.SubscriptionOwner.MetricType);
        Map<String, SubscriptionManager.SubscriptionOwner> tenants = toIdAndOwner(relationships,
                SubscriptionManager.SubscriptionOwner.Tenant);

        for (org.hawkular.datamining.api.model.Metric metric: dataminingMetrics) {
            Set<SubscriptionManager.SubscriptionOwner> subscriptionOwners = new HashSet<>();

            if (metrics.get(metric.getId()) != null) {
                subscriptionOwners.add(SubscriptionManager.SubscriptionOwner.Metric);
            }

            if (metricTypes.get(metric.getMetricType().getPath()) != null) {
                subscriptionOwners.add(SubscriptionManager.SubscriptionOwner.MetricType);
            }

            if (tenants.get(metric.getTenant()) != null) {
                subscriptionOwners.add(SubscriptionManager.SubscriptionOwner.Tenant);
            }

            result.put(metric, subscriptionOwners);
        }

        return result;
    }

    private Map<String, SubscriptionManager.SubscriptionOwner> toIdAndOwner(Set<Relationship> relationships,
                                                                 SubscriptionManager.SubscriptionOwner owner) {
        Map<String, SubscriptionManager.SubscriptionOwner> result = new HashMap<>();

        for (Relationship relationship: relationships) {
            Class<?> target = relationship.getTarget().getSegment().getElementType();

            if (owner.equals(SubscriptionManager.SubscriptionOwner.Metric) && target.equals(Metric.class)) {
                result.put(relationship.getTarget().ids().getMetricId(), SubscriptionManager.SubscriptionOwner.Metric);
            } else if (owner.equals(SubscriptionManager.SubscriptionOwner.MetricType) &&
                    target.equals(MetricType.class)) {
                result.put(relationship.getTarget().toString(),
                        SubscriptionManager.SubscriptionOwner.MetricType);
            } else if (owner.equals(SubscriptionManager.SubscriptionOwner.Tenant) && target.equals(Tenant.class)) {
                result.put(relationship.getTarget().ids().getTenantId(), SubscriptionManager.SubscriptionOwner.Tenant);
            }
        }

        return result;
    }
}
