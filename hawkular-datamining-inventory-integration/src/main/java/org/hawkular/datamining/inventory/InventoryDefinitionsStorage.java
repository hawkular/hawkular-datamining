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
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hawkular.datamining.api.DefinitionsStorage;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.util.Eager;
import org.hawkular.inventory.api.PathFragment;
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
import org.hawkular.inventory.base.spi.NoopFilter;
import org.hawkular.inventory.base.spi.SwitchElementType;

/**
 * @author Pavol Loffay
 */
@Eager
@ApplicationScoped
public class InventoryDefinitionsStorage implements DefinitionsStorage {

    @Inject
    private SubscriptionManager subscriptionManager;


    @PostConstruct
    public void init() {
        getAllMetrics();
    }

    @Override
    public Set<org.hawkular.datamining.api.model.Metric> getAllMetrics() {

        // 1. get relationships
        // 2. get all metrics
        Query queryRelationships = Query.path().with(
                With.type(Tenant.class), SwitchElementType.outgoingRelationships(), RelationWith.name("__inPrediction"),
                RelationWith.targetsOfTypes(Metric.class, MetricType.class)).get();
        InventoryBusQuery<Relationship> relationshipsBusQuery = new InventoryBusQuery<>(queryRelationships);

        Set<Relationship> relationships = relationshipsBusQuery.sendQuery();

        InventoryBusQuery<Metric> metricsBusQuery = new InventoryBusQuery<>(queryAllMetrics(relationships));

        Set<Metric> inventoryMetrics = metricsBusQuery.sendQuery();
        Set<org.hawkular.datamining.api.model.Metric> dataminingMetrics = convertMetrics(inventoryMetrics,
                relationships);

        return dataminingMetrics;
    }


    private Query queryAllMetrics(Set<Relationship> relationships) {
        Set<CanonicalPath> metricsPaths = new HashSet<>();
        Set<CanonicalPath> metricsTypesPaths = new HashSet<>();

        for(Relationship relationship: relationships) {

            CanonicalPath canonicalPath = relationship.getTarget();
            Class<?> entityClass = canonicalPath.getSegment().getElementType();

            if (entityClass.equals(Metric.class)) {
                metricsPaths.add(relationship.getTarget());
            } else if (entityClass.equals(MetricType.class)) {
                metricsTypesPaths.add(relationship.getTarget());
            }
        }

        return queryMetricsByCanonicalPaths(metricsPaths, metricsTypesPaths);
    }

    private Query queryMetricsByCanonicalPaths(Set<CanonicalPath> metrics, Set<CanonicalPath> metricTypes) {

        Query queryTypes = Query.path().with(With.paths(metricTypes.toArray(new CanonicalPath[]{})))
                .with(Related.by(Relationships.WellKnown.defines), With.type(Metric.class)).get();
        Query queryMetrics = Query.path().with(With.paths(metrics.toArray(new CanonicalPath[]{}))).get();

        Query queryAllMetrics = new Query.Builder().with(new PathFragment(new NoopFilter()))
                .branch().with(queryTypes).done()
                .branch().with(queryMetrics).done().build();

        return queryAllMetrics;
    }

    private Set<org.hawkular.datamining.api.model.Metric> convertMetrics(Set<Metric> metrics,
                                                                                  Set<Relationship> relationships) {

        Set<org.hawkular.datamining.api.model.Metric> result = new HashSet<>(metrics.size());

        for (Metric invMetric: metrics) {
            org.hawkular.datamining.api.model.Metric metric = convertMetric(invMetric, relationships);
            result.add(metric);
        }

        return result;
    }

    private org.hawkular.datamining.api.model.Metric convertMetric(Metric invMetric, Set<Relationship> relationships) {
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

    private Long predictionInterval(Set<Relationship> relationships, CanonicalPath targetEntityPath) {

        Long predictionInterval = null;
        for (Relationship relationship: relationships) {

            if (relationship.getTarget().equals(targetEntityPath)) {
                predictionInterval = Long.parseLong((String)relationship.getProperties().get("predictionInterval"));
            }
        }

        return predictionInterval;
    }
}
