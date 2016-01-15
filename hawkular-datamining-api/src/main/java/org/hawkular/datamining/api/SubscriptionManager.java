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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hawkular.datamining.api.model.Metric;

/**
 * @author Pavol Loffay
 */
public interface SubscriptionManager {

    void subscribe(Metric metric, Set<SubscriptionOwner> subscriptionOwner);

    void subscribe(String tenant, TenantSubscriptions tenantSubscriptions);

    boolean subscribes(String tenant, String metricId);

    void unSubscribe(String tenant, String metricId);

    void unSubscribe(String tenant, String metricId, SubscriptionOwner subscriptionOwner);

    void unSubscribe(String tenant, String metricId, Set<SubscriptionOwner> subscriptionOwners);

    Metric subscription(String tenant, String metricId);

    TimeSeriesLinkedModel model(String tenant, String metricId);

    TenantSubscriptions subscriptionsOfTenant(String tenant);

    Set<Metric> metricsOfTenant(String tenant);

    List<TimeSeriesLinkedModel> getAllModels();

    Map<String, TenantSubscriptions> getAllSubscriptions();

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
