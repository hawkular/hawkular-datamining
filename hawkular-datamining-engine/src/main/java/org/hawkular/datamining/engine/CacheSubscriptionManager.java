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

package org.hawkular.datamining.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.TenantSubscriptions;
import org.hawkular.datamining.api.TimeSeriesLinkedModel;
import org.hawkular.datamining.api.exception.SubscriptionNotFoundException;
import org.hawkular.datamining.api.model.DataPoint;
import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.api.storage.MetricStorage;
import org.hawkular.datamining.bus.RestMetricStorage;
import org.hawkular.datamining.engine.model.CombinedTimeSeriesModel;

/**
 * @author Pavol Loffay
 */
@ApplicationScoped
public class CacheSubscriptionManager implements SubscriptionManager {

    // tenant, metricId, model
    private final Map<String, TenantSubscriptions> subscriptions;

    private MetricStorage restMetricStorage = new RestMetricStorage();

    public CacheSubscriptionManager() {
        subscriptions = new HashMap<>();
    }

    @Override
    public TenantSubscriptions subscriptionsOfTenant(String tenant) {
        TenantSubscriptions tenantsSubscriptions = subscriptions.get(tenant);
        if (tenantsSubscriptions == null) {
            tenantsSubscriptions = new TenantSubscriptions();
            subscriptions.put(tenant, tenantsSubscriptions);
        }

        return tenantsSubscriptions;
    }

    @Override
    public Set<Metric> metricsOfTenant(String tenant) {
        TenantSubscriptions tenantSubscriptions = subscriptionsOfTenant(tenant);

        Set<Metric> subscriptions = new HashSet<>();
        for (Map.Entry<String, TimeSeriesLinkedModel> entry: tenantSubscriptions.getSubscriptions().entrySet()) {
            Metric metric = entry.getValue().getLinkedMetric();
            subscriptions.add(metric);
        }

        return subscriptions;
    }

    @Override
    public Map<String, TenantSubscriptions> getAllSubscriptions() {
        return subscriptions;
    }

    @Override
    public void subscribe(Metric metric, Set<SubscriptionOwner> subscriptionOwner) {
        TenantSubscriptions tenantSubscriptions = subscriptions.get(metric.getTenant());
        if (tenantSubscriptions == null) {
            tenantSubscriptions = new TenantSubscriptions();
            subscriptions.put(metric.getTenant(), tenantSubscriptions);
        }

        TimeSeriesLinkedModel model = tenantSubscriptions.getSubscriptions().get(metric.getId());
        if (model != null) {
            model.addAllSubscriptionOwners(subscriptionOwner);
            return;
        }

        model = new CombinedTimeSeriesModel(metric, tenantSubscriptions, subscriptionOwner);

        /**
         * Initialize model with old data
         */
        List<DataPoint> points = restMetricStorage.loadPoints(metric.getId(), metric.getTenant());
        model.addDataPoints(points);

        EngineLogger.LOGGER.subscribing(metric.getId(), metric.getTenant());
        tenantSubscriptions.getSubscriptions().put(metric.getId(), model);
    }

    @Override
    public void subscribe(String tenant, TenantSubscriptions tenantSubscriptions) {
        subscriptions.put(tenant, tenantSubscriptions);
    }

    @Override
    public void unSubscribe(String tenant, String metricId) {
        unSubscribe(tenant, metricId, SubscriptionManager.SubscriptionOwner.getAllDefined());
    }

    @Override
    public void unSubscribe(String tenant, String metricId, SubscriptionOwner subscriptionOwner) {
        unSubscribe(tenant, metricId, new HashSet<>(Arrays.asList(subscriptionOwner)));
    }

    @Override
    public void unSubscribe(String tenant, String metricId, Set<SubscriptionOwner> subscriptionOwners) {
        TenantSubscriptions tenantSubscriptions = subscriptions.get(tenant);
        if (tenantSubscriptions == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        TimeSeriesLinkedModel model = tenantSubscriptions.getSubscriptions().get(metricId);
        if (model == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        for (SubscriptionOwner owner: subscriptionOwners) {
            model.removeSubscriptionOwner(owner);
        }

        if (model.getSubscriptionOwners().isEmpty()) {
            tenantSubscriptions.getSubscriptions().remove(metricId);
        }

        EngineLogger.LOGGER.debugf("Unsubscribed tenant: %s, metric: %s", metricId, tenant);
    }

    @Override
    public boolean subscribes(String tenant, String metricId) {
        TenantSubscriptions tenantsModels = subscriptions.get(tenant);
        if (tenantsModels == null) {
            return false;
        }

        return tenantsModels.getSubscriptions().containsKey(metricId);
    }

    @Override
    public Metric subscription(String tenant, String metricId) {
        TenantSubscriptions tenantsModels = subscriptions.get(tenant);
        if (tenantsModels == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        TimeSeriesLinkedModel model = tenantsModels.getSubscriptions().get(metricId);
        if (model == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        Metric metric = model.getLinkedMetric();
        return metric;
    }

    @Override
    public TimeSeriesLinkedModel model(String tenant, String metricId) {
        TenantSubscriptions tenantsModels = subscriptions.get(tenant);
        if (tenantsModels == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        TimeSeriesLinkedModel timeSeriesLinkedModel = tenantsModels.getSubscriptions().get(metricId);

        if (timeSeriesLinkedModel == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        return timeSeriesLinkedModel;
    }

    @Override
    public List<TimeSeriesLinkedModel> getAllModels() {
        List<TimeSeriesLinkedModel> result = new ArrayList<>();

        for (Map.Entry<String, TenantSubscriptions> tenantEntry: subscriptions.entrySet()) {

            for (Map.Entry<String, TimeSeriesLinkedModel> modelEntry: tenantEntry.getValue()
                    .getSubscriptions().entrySet()) {

                TimeSeriesLinkedModel model = modelEntry.getValue();
                result.add(model);
            }
        }

        return result;
    }
}
