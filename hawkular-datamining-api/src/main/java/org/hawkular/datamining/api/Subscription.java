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
package org.hawkular.datamining.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.hawkular.datamining.api.base.DataMiningForecaster;
import org.hawkular.datamining.forecast.MetricContext;

/**
 * Metric can subscribe for functionality offered by Data Mining module. Currently is only time series prediction
 * available (through {@link #forecaster()})
 *
 * @author Pavol Loffay
 */
public interface Subscription {

    /**
     * @return associated metric with subscription
     */
    MetricContext getMetric();

    Set<SubscriptionOwner> getSubscriptionOwners();

    void addSubscriptionOwner(SubscriptionOwner owner);

    void addSubscriptionOwners(Set<SubscriptionOwner> owners);

    void removeSubscriptionOwner(SubscriptionOwner owner);

    DataMiningForecaster forecaster();

    /**
     * Represents relationship from Tenant to:
     */
    enum SubscriptionOwner {
        Tenant,
        MetricType,
        Metric;

        public static Set<SubscriptionOwner> getAllDefined() {
            return new HashSet<>(Arrays.asList(Tenant, MetricType, Metric));
        }
    }
}
