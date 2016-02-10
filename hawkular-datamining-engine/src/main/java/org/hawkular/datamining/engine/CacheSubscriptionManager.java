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

import org.hawkular.datamining.api.Subscription;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.TenantSubscriptions;
import org.hawkular.datamining.api.exception.SubscriptionNotFoundException;
import org.hawkular.datamining.api.model.DataPoint;
import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.api.storage.MetricsClient;
import org.hawkular.datamining.bus.RestMetricsClient;

/**
 * @author Pavol Loffay
 */
@ApplicationScoped
public class CacheSubscriptionManager implements SubscriptionManager {

    // tenant, metricId, model
    private final Map<String, TenantSubscriptions> subscriptions;

    private MetricsClient restMetricsClient = new RestMetricsClient();

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
        for (Map.Entry<String, Subscription> entry : tenantSubscriptions.getSubscriptions().entrySet()) {
            Metric metric = entry.getValue().getMetric();
            subscriptions.add(metric);
        }

        return subscriptions;
    }

    @Override
    public Map<String, TenantSubscriptions> getAllSubscriptions() {
        return subscriptions;
    }

    @Override
    public void subscribe(Metric metric, Set<ModelOwner> modelOwner) {
        TenantSubscriptions tenantSubscriptions = subscriptions.get(metric.getTenant());
        if (tenantSubscriptions == null) {
            tenantSubscriptions = new TenantSubscriptions();
            subscriptions.put(metric.getTenant(), tenantSubscriptions);
        }

        Subscription subscription = tenantSubscriptions.getSubscriptions().get(metric.getId());
        if (subscription != null) {
            subscription.addAllSubscriptionOwners(modelOwner);
            return;
        }

        subscription = new TimeSeriesSubscription(metric, tenantSubscriptions, modelOwner);

        /**
         * Initialize subscription with old data
         */
        List<DataPoint> points = restMetricsClient.loadPoints(metric.getId(), metric.getTenant());
        subscription.forecaster().learn(points);

        EngineLogger.LOGGER.subscribing(metric.getId(), metric.getTenant());
        tenantSubscriptions.getSubscriptions().put(metric.getId(), subscription);
    }

    @Override
    public void subscribe(String tenant, TenantSubscriptions tenantSubscriptions) {
        subscriptions.put(tenant, tenantSubscriptions);
    }

    @Override
    public void unSubscribe(String tenant, String metricId) {
        unSubscribe(tenant, metricId, ModelOwner.getAllDefined());
    }

    @Override
    public void unSubscribe(String tenant, String metricId, ModelOwner modelOwner) {
        unSubscribe(tenant, metricId, new HashSet<>(Arrays.asList(modelOwner)));
    }

    @Override
    public void unSubscribe(String tenant, String metricId, Set<ModelOwner> modelOwners) {
        TenantSubscriptions tenantSubscriptions = subscriptions.get(tenant);
        if (tenantSubscriptions == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        Subscription model = tenantSubscriptions.getSubscriptions().get(metricId);
        if (model == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        for (ModelOwner owner : modelOwners) {
            model.removeSubscriptionOwner(owner);
        }

        if (model.getModelOwners().isEmpty()) {
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
    public Metric metric(String tenant, String metricId) {
        TenantSubscriptions tenantsModels = subscriptions.get(tenant);
        if (tenantsModels == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        Subscription model = tenantsModels.getSubscriptions().get(metricId);
        if (model == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        Metric metric = model.getMetric();
        return metric;
    }

    @Override
    public Subscription subscription(String tenant, String metricId) {
        TenantSubscriptions tenantsModels = subscriptions.get(tenant);
        if (tenantsModels == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        Subscription subscription = tenantsModels.getSubscriptions().get(metricId);

        if (subscription == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        return subscription;
    }

    @Override
    public List<Subscription> getAllModels() {
        List<Subscription> result = new ArrayList<>();

        for (Map.Entry<String, TenantSubscriptions> tenantEntry : subscriptions.entrySet()) {

            for (Map.Entry<String, Subscription> modelEntry : tenantEntry.getValue()
                    .getSubscriptions().entrySet()) {

                Subscription model = modelEntry.getValue();
                result.add(model);
            }
        }

        return result;
    }
}
