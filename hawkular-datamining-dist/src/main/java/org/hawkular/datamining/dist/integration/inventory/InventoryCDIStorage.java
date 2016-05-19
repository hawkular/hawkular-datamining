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

package org.hawkular.datamining.dist.integration.inventory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hawkular.datamining.api.Subscription;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.base.DataMiningForecaster;
import org.hawkular.datamining.api.base.DataMiningSubscription;
import org.hawkular.datamining.cdi.qualifiers.Eager;
import org.hawkular.datamining.dist.Logger;
import org.hawkular.datamining.dist.integration.Configuration;
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

    private PredictionRelationships predictionRelationships;

    @PostConstruct
    public void init() {
        predictionRelationships = new PredictionRelationships();

        Set<Metric> predictedMetrics = getAllPredictedMetrics();

        predictedMetrics.forEach(metric -> {
            CanonicalPath tenantCp = metric.getPath().getRoot();
            CanonicalPath metricTypeCp = metric.getType().getPath();
            CanonicalPath metricCp = metric.getPath();


            Set<Subscription.SubscriptionOwner> subscriptionOwners = new HashSet<>();
            CanonicalPath forecastingHorizonCp = null;
            if (predictionRelationships.relationships().get(tenantCp) != null) {
                subscriptionOwners.add(Subscription.SubscriptionOwner.Tenant);
                forecastingHorizonCp = tenantCp;
            }
            if (predictionRelationships.relationships().get(metricTypeCp) != null) {
                subscriptionOwners.add(Subscription.SubscriptionOwner.MetricType);
                forecastingHorizonCp = metricTypeCp;
            }
            if (predictionRelationships.relationships().get(metricCp) != null) {
                subscriptionOwners.add(Subscription.SubscriptionOwner.Metric);
                forecastingHorizonCp = metricCp;
            }

            final Long forecastingHorizon = InventoryUtil.parseForecastingHorizon(
                    predictionRelationships.relationships().get(forecastingHorizonCp));

            org.hawkular.datamining.api.model.Metric dataMMetric = InventoryUtil.convertMetric(metric,
                    forecastingHorizon);

            final Subscription subscription = new DataMiningSubscription(
                    new DataMiningForecaster(dataMMetric), subscriptionOwners);

            subscriptionManager.subscribe(subscription);
        });

        Logger.LOGGER.inventoryInitialized(predictedMetrics.size());
    }

    @Override
    public Set<Relationship> predictionRelationships(CanonicalPath... targetEntity) {

        Set<Relationship> result = new HashSet<>();
        for (CanonicalPath target: targetEntity) {
            Relationship relationship  = predictionRelationships.relationships().get(target);

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
        return metrics.isEmpty() ? null : metrics.get(0) ;
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
        predictionRelationships.relationships().put(relationship.getTarget(), relationship);
    }

    @Override
    public void removePredictionRelationship(Relationship relationship) {
        predictionRelationships.relationships().remove(relationship.getTarget());
    }

    private Set<Metric> getAllPredictedMetrics() {

        Set<Relationship> relationships =
                inventory.relationships().named(Configuration.PREDICTION_RELATIONSHIP).entities();

        Set<CanonicalPath> metricsCp = new HashSet<>();
        Set<CanonicalPath> metricTypesCp = new HashSet<>();
        Set<CanonicalPath> tenantsCp = new HashSet<>();

        for (Relationship relationship : relationships) {

            predictionRelationships.relationships().put(relationship.getTarget(), relationship);

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

        return metrics;
    }


    private static class PredictionRelationships {

        // <targetEntity, relationship>
        private Map<CanonicalPath, Relationship> targetEntityRelationships = new ConcurrentHashMap<>();

        public Map<CanonicalPath, Relationship> relationships() {
            return targetEntityRelationships;
        }
    }
}
