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

import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

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
import org.hawkular.inventory.api.model.Relationship;
import org.hawkular.inventory.base.spi.NoopFilter;
import org.hawkular.inventory.base.spi.SwitchElementType;

/**
 * @author Pavol Loffay
 */
@Eager
@ApplicationScoped
public class InventoryBusStorage implements InventoryStorage {

    @Inject
    private SubscriptionManager subscriptionManager;


    @PostConstruct
    public void init() {
        InventoryInitializer inventoryInitializer = new InventoryInitializer();

        Set<org.hawkular.datamining.api.model.Metric> predictedMetrics =
                inventoryInitializer.getAllPredictedMetrics();

        predictedMetrics.forEach(x -> subscriptionManager.subscribe(x));
    }

    @Override
    public Set<Relationship> predictionRelationships(CanonicalPath metric, CanonicalPath metricType) {
        Query qMetricsRelationships = Query.path().with(
                With.path(metric),
                SwitchElementType.incomingRelationships(),
                RelationWith.name(InventoryConfiguration.PREDICTION_RELATIONSHIP)).get();

        Query qTypesRelationships = Query.path().with(
                With.path(metricType),
                SwitchElementType.incomingRelationships(),
                RelationWith.name(InventoryConfiguration.PREDICTION_RELATIONSHIP)).get();

        Query queryAllMetrics = new Query.Builder().with(new PathFragment(new NoopFilter()))
                .branch().with(qMetricsRelationships).done()
                .branch().with(qTypesRelationships).done().build();

        InventoryBusClient<Relationship> busClient = new InventoryBusClient<>(queryAllMetrics);
        Set<Relationship> relationships = busClient.sendQuery();

        return relationships;
    }

    @Override
    public Set<Metric> metricsOfType(CanonicalPath metricType) {
        Query query = Query.path().with(With.path(metricType))
                .with(Related.by(Relationships.WellKnown.defines), With.type(Metric.class)).get();

        InventoryBusClient<Metric> busClient = new InventoryBusClient<>(query);
        Set<Metric> metrics = busClient.sendQuery();

        return metrics;
    }

    @Override
    public Metric metric(CanonicalPath metric) {
        Query query = Query.path().with(With.path(metric)).get();

        InventoryBusClient<Metric> busClient = new InventoryBusClient<>(query);
        Set<Metric> metrics = busClient.sendQuery();

        return metrics.isEmpty() == true ? null : metrics.iterator().next();
    }
}
