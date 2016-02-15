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

import java.util.HashSet;
import java.util.Set;

import org.hawkular.datamining.api.Subscription;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.TenantSubscriptions;
import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.forecast.AutomaticForecaster;
import org.hawkular.datamining.forecast.Forecaster;

/**
 * @author Pavol Loffay
 */
public class TimeSeriesSubscription implements Subscription {

    private final Metric metric;
    private final TenantSubscriptions tenantSubscriptions;
    private final Set<SubscriptionManager.ModelOwner> modelOwners = new HashSet<>();

    private final Forecaster forecaster;


    public TimeSeriesSubscription(Metric metric, TenantSubscriptions tenantSubscriptions,
                                  Set<SubscriptionManager.ModelOwner> modelOwner) {
        this.metric = metric;
        this.tenantSubscriptions = tenantSubscriptions;
        this.modelOwners.addAll(modelOwner);

        this.forecaster = new AutomaticForecaster();
    }

    @Override
    public Metric getMetric() {
        return metric;
    }

    @Override
    public Long getForecastingHorizon() {
        Long forecastingHorizon = null;

        if (modelOwners.contains(SubscriptionManager.ModelOwner.Metric)) {
            forecastingHorizon = metric.getForecastingHorizon();
        } else if (modelOwners.contains(SubscriptionManager.ModelOwner.MetricType)) {
            forecastingHorizon = metric.getMetricType().getForecastingHorizon();
        } else {
            forecastingHorizon = tenantSubscriptions.getForecastingHorizon();
        }

        return forecastingHorizon;
    }

    @Override
    public Long getCollectionInterval() {
        Long collectionInterval = null;

        if (metric.getCollectionInterval() != null) {
            collectionInterval = metric.getCollectionInterval();
        } else if (metric.getMetricType() != null) {
            collectionInterval = metric.getMetricType().getCollectionInterval();
        }

        return collectionInterval;
    }

    @Override
    public Set<SubscriptionManager.ModelOwner> getModelOwners() {
        return new HashSet<>(modelOwners);
    }

    @Override
    public Forecaster forecaster() {
        return forecaster;
    }

    @Override
    public void addSubscriptionOwner(SubscriptionManager.ModelOwner modelOwner) {
        this.modelOwners.add(modelOwner);
    }

    @Override
    public void addAllSubscriptionOwners(Set<SubscriptionManager.ModelOwner> owners) {
        this.modelOwners.addAll(owners);
    }

    @Override
    public void removeSubscriptionOwner(SubscriptionManager.ModelOwner modelOwner) {
        this.modelOwners.remove(modelOwner);
    }
}
