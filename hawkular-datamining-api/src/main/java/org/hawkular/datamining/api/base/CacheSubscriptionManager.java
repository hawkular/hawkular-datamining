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

package org.hawkular.datamining.api.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawkular.datamining.api.Logger;
import org.hawkular.datamining.api.PredictionListener;
import org.hawkular.datamining.api.Subscription;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.exception.SubscriptionAlreadyExistsException;
import org.hawkular.datamining.api.exception.SubscriptionNotFoundException;
import org.hawkular.datamining.api.storage.MetricsClient;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.MetricContext;

/**
 * @author Pavol Loffay
 */
public class CacheSubscriptionManager implements SubscriptionManager {

    // tenant, metricId, model
    private final Map<String, TenantsSubscriptionsHolder> subscriptions;

    private final MetricsClient restMetricsClient;

    private PredictionListener predictionListener;


    public CacheSubscriptionManager(MetricsClient metricsClient) {
        this.subscriptions = new HashMap<>();
        this.restMetricsClient = metricsClient;
    }

    @Override
    public TenantsSubscriptionsHolder subscriptionsOfTenant(String tenant) {
        TenantsSubscriptionsHolder tenantsSubscriptions = subscriptions.get(tenant);
        if (tenantsSubscriptions == null) {
            tenantsSubscriptions = new TenantsSubscriptionsHolder();
            subscriptions.put(tenant, tenantsSubscriptions);
        }

        return tenantsSubscriptions;
    }

    @Override
    public Set<? extends MetricContext> metricsOfTenant(String tenant) {
        TenantsSubscriptionsHolder tenantSubscriptions = subscriptionsOfTenant(tenant);

        Set<MetricContext> subscriptions = new HashSet<>();
        for (Map.Entry<String, Subscription> entry : tenantSubscriptions.getSubscriptions().entrySet()) {
            MetricContext metric = entry.getValue().getMetric();
            subscriptions.add(metric);
        }

        return subscriptions;
    }

    @Override
    public Map<String, TenantsSubscriptionsHolder> getAllSubscriptions() {
        return subscriptions;
    }

    @Override
    public void setPredictionListener(PredictionListener predictionListener) {
        this.predictionListener = predictionListener;
    }

    @Override
    public void subscribe(Subscription subscription) {
        TenantsSubscriptionsHolder tenantSubscriptions = subscriptions.get(subscription.getMetric().getTenant());
        if (tenantSubscriptions == null) {
            tenantSubscriptions = new TenantsSubscriptionsHolder();
            subscriptions.put(subscription.getMetric().getTenant(), tenantSubscriptions);
        }

        if (tenantSubscriptions.getSubscriptions().get(subscription) != null) {
            throw new SubscriptionAlreadyExistsException();
        }

        /**
         * Initialize subscription with old data
         */
        List<DataPoint> points = restMetricsClient.loadPoints(subscription.getMetric().getMetricId(),
                subscription.getMetric().getTenant());
        subscription.forecaster().learn(points);

        Logger.LOGGER.subscribing(subscription.getMetric().getMetricId(), subscription.getMetric().getTenant());
        tenantSubscriptions.getSubscriptions().put(subscription.getMetric().getMetricId(), subscription);
    }

    @Override
    public void unSubscribeAll(String tenant, String metricId) {
        unSubscribe(tenant, metricId, Subscription.SubscriptionOwner.getAllDefined());
    }

    @Override
    public void unSubscribe(String tenant, String metricId, Subscription.SubscriptionOwner subscriptionOwner) {
        unSubscribe(tenant, metricId, new HashSet<>(Arrays.asList(subscriptionOwner)));
    }

    @Override
    public void unSubscribe(String tenant, String metricId, Set<Subscription.SubscriptionOwner> subscriptionOwners) {
        TenantsSubscriptionsHolder tenantSubscriptions = subscriptions.get(tenant);
        if (tenantSubscriptions == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        Subscription model = tenantSubscriptions.getSubscriptions().get(metricId);
        if (model == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        for (Subscription.SubscriptionOwner owner : subscriptionOwners) {
            model.removeSubscriptionOwner(owner);
        }

        if (model.getSubscriptionOwners().isEmpty()) {
            tenantSubscriptions.getSubscriptions().remove(metricId);
        }

        Logger.LOGGER.debugf("Unsubscribed tenant: %s, metric: %s", metricId, tenant);
    }

    @Override
    public boolean subscribes(String tenant, String metricId) {
        TenantsSubscriptionsHolder tenantsModels = subscriptions.get(tenant);
        if (tenantsModels == null) {
            return false;
        }

        return tenantsModels.getSubscriptions().containsKey(metricId);
    }

    @Override
    public Subscription subscription(String tenant, String metricId) {
        TenantsSubscriptionsHolder tenantsModels = subscriptions.get(tenant);
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

        for (Map.Entry<String, TenantsSubscriptionsHolder> tenantEntry : subscriptions.entrySet()) {

            for (Map.Entry<String, Subscription> modelEntry : tenantEntry.getValue()
                    .getSubscriptions().entrySet()) {

                Subscription model = modelEntry.getValue();
                result.add(model);
            }
        }

        return result;
    }
}
