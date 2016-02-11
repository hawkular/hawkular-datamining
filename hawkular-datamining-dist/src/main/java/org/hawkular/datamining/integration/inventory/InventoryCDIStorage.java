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

package org.hawkular.datamining.integration.inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.TenantSubscriptions;
import org.hawkular.datamining.api.util.Eager;
import org.hawkular.datamining.integration.Configuration;
import org.hawkular.datamining.integration.Logger;
import org.hawkular.inventory.api.Inventory;
import org.hawkular.inventory.api.Query;
import org.hawkular.inventory.api.Relationships;
import org.hawkular.inventory.api.filters.Related;
import org.hawkular.inventory.api.filters.With;
import org.hawkular.inventory.api.model.CanonicalPath;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.Relationship;
import org.hawkular.inventory.api.model.Tenant;
import org.hawkular.inventory.api.paging.Order;
import org.hawkular.inventory.api.paging.Page;
import org.hawkular.inventory.api.paging.Pager;

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

    private org.hawkular.datamining.integration.inventory.PredictionRelationshipsCache predictionRelationshipsCache;

    @PostConstruct
    public void init() {
        predictionRelationshipsCache = new PredictionRelationshipsCache();

        Map<org.hawkular.datamining.api.model.Metric, Set<SubscriptionManager.ModelOwner>> allPredictedMetrics =
                getAllPredictedMetrics();

        for (Map.Entry<org.hawkular.datamining.api.model.Metric, Set<SubscriptionManager.ModelOwner>> entry:
                allPredictedMetrics.entrySet()) {

            subscriptionManager.subscribe(entry.getKey(), entry.getValue());
        }

      Logger.LOGGER.inventoryInitialized(allPredictedMetrics.size());
    }

    @Override
    public Set<Relationship> predictionRelationships(CanonicalPath... targetEntity) {

//        Query query = Query.path().with(With.paths(targetEntity)).with(SwitchElementType.incomingRelationships(),
//                        RelationWith.name(InventoryConfiguration.PREDICTION_RELATIONSHIP)).get();

//        Page<Relationship> page = inventory.execute(query, Relationship.class, Pager.unlimited(Order.unspecified()));

        Set<Relationship> result = new HashSet<>();
        for (CanonicalPath target: targetEntity) {
            Relationship relationship  = predictionRelationshipsCache.relationships().get(target);

            if (relationship != null) {
                result.add(relationship);
            }
        }

        return result;
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

    @Override
    public void addPredictionRelationship(Relationship relationship) {
        predictionRelationshipsCache.relationships().put(relationship.getTarget(), relationship);
    }

    @Override
    public void removePredictionRelationship(Relationship relationship) {
        predictionRelationshipsCache.relationships().remove(relationship);
    }

    private Map<org.hawkular.datamining.api.model.Metric, Set<SubscriptionManager.ModelOwner>>
    getAllPredictedMetrics() {

        Set<Relationship> relationships =
                inventory.relationships().named(Configuration.PREDICTION_RELATIONSHIP).entities();

        Set<CanonicalPath> metricsCp = new HashSet<>();
        Set<CanonicalPath> metricTypesCp = new HashSet<>();
        Set<CanonicalPath> tenantsCp = new HashSet<>();

        for (Relationship relationship: relationships) {

            predictionRelationshipsCache.relationships().put(relationship.getTarget(), relationship);

            if (relationship.getTarget().getSegment().getElementType().equals(Metric.class)) {
                metricsCp.add(relationship.getTarget());
            } else if (relationship.getTarget().getSegment().getElementType().equals(MetricType.class)) {
                metricTypesCp.add(relationship.getTarget());
            } else if (relationship.getTarget().getSegment().getElementType().equals(Tenant.class)) {

                TenantSubscriptions tenantSubscriptions = new TenantSubscriptions(InventoryUtil
                        .parseForecastingHorizon(relationship.getProperties()));
                subscriptionManager.subscribe(relationship.getTarget().ids().getTenantId(), tenantSubscriptions);

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

    private Map<org.hawkular.datamining.api.model.Metric, Set<SubscriptionManager.ModelOwner>>
    metricsWithOwnersOfSubscription(Set<Relationship> relationships,
                                    Set<org.hawkular.datamining.api.model.Metric> dataminingMetrics) {

        Map<org.hawkular.datamining.api.model.Metric, Set<SubscriptionManager.ModelOwner>> result =
                new HashMap<>();

        Map<String, SubscriptionManager.ModelOwner> metrics = toIdAndOwner(relationships,
                SubscriptionManager.ModelOwner.Metric);
        Map<String, SubscriptionManager.ModelOwner> metricTypes = toIdAndOwner(relationships,
                SubscriptionManager.ModelOwner.MetricType);
        Map<String, SubscriptionManager.ModelOwner> tenants = toIdAndOwner(relationships,
                SubscriptionManager.ModelOwner.Tenant);

        for (org.hawkular.datamining.api.model.Metric metric: dataminingMetrics) {
            Set<SubscriptionManager.ModelOwner> modelOwners = new HashSet<>();

            if (metrics.get(metric.getId()) != null) {
                modelOwners.add(SubscriptionManager.ModelOwner.Metric);
            }

            if (metricTypes.get(metric.getMetricType().getPath()) != null) {
                modelOwners.add(SubscriptionManager.ModelOwner.MetricType);
            }

            if (tenants.get(metric.getTenant()) != null) {
                modelOwners.add(SubscriptionManager.ModelOwner.Tenant);
            }

            result.put(metric, modelOwners);
        }

        return result;
    }

    private Map<String, SubscriptionManager.ModelOwner> toIdAndOwner(Set<Relationship> relationships,
                                                                     SubscriptionManager.ModelOwner owner) {
        Map<String, SubscriptionManager.ModelOwner> result = new HashMap<>();

        for (Relationship relationship: relationships) {
            Class<?> target = relationship.getTarget().getSegment().getElementType();

            if (owner.equals(SubscriptionManager.ModelOwner.Metric) && target.equals(Metric.class)) {
                result.put(relationship.getTarget().ids().getMetricId(), SubscriptionManager.ModelOwner.Metric);
            } else if (owner.equals(SubscriptionManager.ModelOwner.MetricType) &&
                    target.equals(MetricType.class)) {
                result.put(relationship.getTarget().toString(),
                        SubscriptionManager.ModelOwner.MetricType);
            } else if (owner.equals(SubscriptionManager.ModelOwner.Tenant) && target.equals(Tenant.class)) {
                result.put(relationship.getTarget().ids().getTenantId(), SubscriptionManager.ModelOwner.Tenant);
            }
        }

        return result;
    }
}
