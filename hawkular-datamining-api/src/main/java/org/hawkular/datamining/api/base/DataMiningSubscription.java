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
import org.hawkular.datamining.forecast.MetricContext;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Pavol Loffay
 */
public class DataMiningSubscription implements Subscription {

    private final Set<SubscriptionOwner> subscriptionOwners = new HashSet<>();

    @JsonProperty
    private final DataMiningForecaster forecaster;


    public DataMiningSubscription(DataMiningForecaster forecaster, Set<SubscriptionOwner> subscriptionOwner) {
        if (subscriptionOwner == null || subscriptionOwner.isEmpty()) {
            throw new IllegalArgumentException("Forecaster should have at least one owner");
        }

        this.forecaster = forecaster;
        this.subscriptionOwners.addAll(subscriptionOwner);
    }

    @Override
    public MetricContext getMetric() {
        return forecaster.context();
    }

    @Override
    public DataMiningForecaster forecaster() {
        return forecaster;
    }

    public Set<SubscriptionOwner> getSubscriptionOwners() {
        return new HashSet<>(subscriptionOwners);
    }

    @Override
    public void addSubscriptionOwner(SubscriptionOwner subscriptionOwner) {
        this.subscriptionOwners.add(subscriptionOwner);
    }

    @Override
    public void addAllSubscriptionOwners(Set<SubscriptionOwner> owners) {
        this.subscriptionOwners.addAll(owners);
    }

    @Override
    public void removeSubscriptionOwner(SubscriptionOwner subscriptionOwner) {
        this.subscriptionOwners.remove(subscriptionOwner);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataMiningSubscription)) return false;

        DataMiningSubscription that = (DataMiningSubscription) o;

        return !(forecaster().context() != null ?
                !forecaster().context().getMetricId().equals(that.forecaster().context().getMetricId()) :
                that.forecaster().context().getMetricId() != null);
    }

    @Override
    public int hashCode() {
        return forecaster().context().getMetricId() != null ? forecaster().context().getMetricId().hashCode() : 0;
    }
}
