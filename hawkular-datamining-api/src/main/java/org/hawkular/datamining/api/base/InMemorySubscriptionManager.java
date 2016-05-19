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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.hawkular.datamining.api.Logger;
import org.hawkular.datamining.api.PredictionListener;
import org.hawkular.datamining.api.Subscription;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.exception.SubscriptionAlreadyExistsException;
import org.hawkular.datamining.api.exception.SubscriptionNotFoundException;
import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.api.storage.MetricsClient;
import org.hawkular.datamining.forecast.DataPoint;

/**
 * @author Pavol Loffay
 */
public class InMemorySubscriptionManager implements SubscriptionManager {

    // tenant, metricId, model
    private final Map<String, TenantsSubscriptionsHolder> subscriptions = new ConcurrentHashMap<>();

    private final MetricsClient restMetricsClient;

    private PredictionListener predictionListener;


    public InMemorySubscriptionManager(MetricsClient metricsClient) {
        this.restMetricsClient = metricsClient;
    }

    @Override
    public Set<Subscription> subscriptionsOfTenant(String tenant) {
        TenantsSubscriptionsHolder tenantsSubscriptionsHolder = subscriptions.get(tenant);
        if (tenantsSubscriptionsHolder == null) {
            return Collections.emptySet();
        }

        return new HashSet<>(tenantsSubscriptionsHolder.getSubscriptions().values());
    }

    @Override
    public void setPredictionListener(PredictionListener predictionListener) {
        this.predictionListener = predictionListener;
    }

    @Override
    public void subscribe(Subscription subscription) {

        TenantsSubscriptionsHolder tenantSubscriptions;

        synchronized (subscriptions) {
            tenantSubscriptions = subscriptions.get(subscription.getMetric().getTenant());
            if (tenantSubscriptions == null) {
                tenantSubscriptions = new TenantsSubscriptionsHolder();
                subscriptions.put(subscription.getMetric().getTenant(), tenantSubscriptions);
            }

            if (tenantSubscriptions.getSubscriptions().get(subscription.getMetric().getMetricId()) != null) {
                throw new SubscriptionAlreadyExistsException();
            }
        }

        /**
         * Set prediction listener
         */
        subscription.forecaster().setPredictionListener(predictionListener);

        /**
         * Initialize subscription with old data
         */
        long startTime = System.currentTimeMillis() - subscription.getMetric().getCollectionInterval() * 100 * 1000;
        List<DataPoint> points = restMetricsClient.loadPoints((Metric) subscription.getMetric(), startTime,
                System.currentTimeMillis());
        subscription.forecaster().learn(points);

        tenantSubscriptions.getSubscriptions().put(subscription.getMetric().getMetricId(), subscription);
        Logger.LOGGER.subscribe(subscription.getMetric().getMetricId(), subscription.getMetric().getTenant());
    }

    @Override
    public void updateMetric(String tenant, String metricId, Metric.Update update) {
        Subscription subscription = subscription(tenant, metricId);
        updateMetric(update, (Metric)subscription.getMetric());
    }

    @Override
    public void updateForecaster(String tenant, String metricId,
                                 org.hawkular.datamining.forecast.Forecaster.Update update) {

        Subscription subscription = subscription(tenant, metricId);
        subscription.forecaster().update(update);
    }

    @Override
    public void unsubscribeAll(String tenant, String metricId) {
        unsubscribe(tenant, metricId, Subscription.SubscriptionOwner.getAllDefined());
    }

    @Override
    public void unsubscribe(String tenant, String metricId, Subscription.SubscriptionOwner subscriptionOwner) {
        unsubscribe(tenant, metricId, Collections.singleton(subscriptionOwner));
    }

    @Override
    public void unsubscribe(String tenant, String metricId, Set<Subscription.SubscriptionOwner> subscriptionOwners) {

        TenantsSubscriptionsHolder tenantSubscriptions = subscriptions.get(tenant);
        if (tenantSubscriptions == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        Subscription subscription = tenantSubscriptions.getSubscriptions().get(metricId);
        if (subscription == null) {
            throw new SubscriptionNotFoundException(tenant, metricId);
        }

        subscriptionOwners.forEach(subscription::removeSubscriptionOwner);

        if (subscription.getSubscriptionOwners().isEmpty()) {
            tenantSubscriptions.getSubscriptions().remove(metricId);
            Logger.LOGGER.debugf("Unsubscribed tenant: %s, metric: %s", tenant, metricId);
        }
    }

    @Override
    public boolean isSubscribed(String tenant, String metricId) {
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

    private static Metric updateMetric(Metric.Update update, Metric metric) {
        if (update.getCollectionInterval() != null) {
            metric.setCollectionInterval(update.getCollectionInterval());
        }
        if (update.getForecastingHorizon() != null) {
            metric.setForecastingHorizon(update.getForecastingHorizon());
        }

        return metric;
    }

    private static class TenantsSubscriptionsHolder {

        // <metricId, subscription (model)>
        private Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

        public Map<String, Subscription> getSubscriptions() {
            return subscriptions;
        }
    }
}
