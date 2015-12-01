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

package org.hawkular.datamining.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.hawkular.datamining.api.ModelSubscription;
import org.hawkular.datamining.api.TimeSeriesLinkedModel;
import org.hawkular.datamining.api.exception.SubscriptionAlreadyExistsException;
import org.hawkular.datamining.api.exception.SubscriptionNotFoundException;
import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.api.model.MetricType;
import org.hawkular.datamining.engine.model.CombinedTimeSeriesModel;

/**
 * @author Pavol Loffay
 */
@ApplicationScoped
public class SubscriptionManager implements ModelSubscription {

    // todo should not be static
    private static final Map<String, Map<String, TimeSeriesLinkedModel>> subscriptions = new HashMap<>();

    // TODO REMOVE
    public static final String TENANT = "28026b36-8fe4-4332-84c8-524e173a68bf";
    public static final String HEAP_USED_METRICS = "MI~R~[dhcp130-144~Local~~]~MT~WildFly Memory Metrics~Heap Used";
    static {
        Map<String, TimeSeriesLinkedModel> metricModels = new HashMap<>();
        metricModels.put(HEAP_USED_METRICS, new CombinedTimeSeriesModel(
                new Metric(TENANT, HEAP_USED_METRICS, 60L, new MetricType(30L), 60L * 60L)));

        subscriptions.put(TENANT, metricModels);
    }

    @Override
    public Set<Metric> getSubscriptions(String tenant) {
        Map<String, TimeSeriesLinkedModel> tenantsSubscriptions = subscriptions.get(tenant);
        if (tenantsSubscriptions == null) {
            throw new SubscriptionNotFoundException(tenant);
        }

        Set<Metric> subscriptions = new HashSet<>();
        for (Map.Entry<String, TimeSeriesLinkedModel> entry: tenantsSubscriptions.entrySet()) {
            Metric metric = entry.getValue().getLinkedMetric();
            subscriptions.add(metric);
        }

        return subscriptions;
    }

    @Override
    public Map<String, Map<String, TimeSeriesLinkedModel>> getAllSubscriptions() {
        return subscriptions;
    }

    @Override
    public void subscribe(Metric metric) {
        Map<String, TimeSeriesLinkedModel> tenantsModels = subscriptions.get(metric.getTenant());
        if (tenantsModels == null) {
            tenantsModels = new HashMap<>();
        }

        TimeSeriesLinkedModel model = tenantsModels.get(metric.getId());
        if (model != null) {
            throw new SubscriptionAlreadyExistsException();
        }

        model = new CombinedTimeSeriesModel(metric);

        tenantsModels.put(metric.getId(), model);
    }

    @Override
    public void unSubscribe(String tenant, String metricId) {
        Map<String, TimeSeriesLinkedModel> tenantsModels = subscriptions.get(tenant);
        if (tenantsModels == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        if (tenantsModels.remove(metricId) == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }
    }

    @Override
    public boolean subscribes(String tenant, String metricId) {
        Map<String, TimeSeriesLinkedModel> tenantsModels = subscriptions.get(tenant);
        if (tenantsModels == null) {
            return false;
        }

        return tenantsModels.containsKey(metricId);
    }

    @Override
    public Metric subscription(String tenant, String metricId) {
        Map<String, TimeSeriesLinkedModel> tenantsModels = subscriptions.get(tenant);
        if (tenantsModels == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        TimeSeriesLinkedModel model = tenantsModels.get(metricId);
        if (model == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        Metric metric = model.getLinkedMetric();
        return metric;
    }

    @Override
    public TimeSeriesLinkedModel getModel(String tenant, String metricId) {
        Map<String, TimeSeriesLinkedModel> tenantsModels = subscriptions.get(tenant);
        if (tenantsModels == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        return tenantsModels.get(metricId);
    }

    @Override
    public List<TimeSeriesLinkedModel> getAllModels() {
        List<TimeSeriesLinkedModel> result = new ArrayList<>();

        for (Map.Entry<String, Map<String, TimeSeriesLinkedModel>> tenantEntry: subscriptions.entrySet()) {
            String tenant = tenantEntry.getKey();

            for (Map.Entry<String, TimeSeriesLinkedModel> modelEntry: tenantEntry.getValue().entrySet()) {
                String metric = modelEntry.getKey();
                TimeSeriesLinkedModel model = modelEntry.getValue();

                result.add(model);
            }
        }

        return result;
    }
}
