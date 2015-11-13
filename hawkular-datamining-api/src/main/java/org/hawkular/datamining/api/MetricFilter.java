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

package org.hawkular.datamining.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Pavol Loffay
 */
public class MetricFilter {

    private static final Map<String, Set<String>> subscriptions = new HashMap<>();


    // TODO REMOVE
    public static final String TENANT = "28026b36-8fe4-4332-84c8-524e173a68bf";
    public static final String HEAP_USED_METRICS = "MI~R~[dhcp130-144~Local~~]~MT~WildFly Memory Metrics~Heap Used";
    static {
        Set<String> metrics = new HashSet<>();
        metrics.add(HEAP_USED_METRICS);

        subscriptions.put(TENANT, metrics);
    }


    public static boolean subscribe(String tenant, String metricsId) {
        Set<String> tenantsSubscriptions = getTenantSubscription(tenant);
        return tenantsSubscriptions.add(metricsId);
    }

    public static boolean unSubscribe(String tenant, String metricsId) {
        Set<String> tenantSubscriptions = getTenantSubscription(tenant);
        return tenantSubscriptions.remove(metricsId);
    }

    public static boolean contains(String tenant, String metricsId) {
        return getTenantSubscription(tenant)
                .contains(metricsId);
    }

    public static Set<String> getTenantSubscription(String tenant) {
        Set<String> subscription = subscriptions.get(tenant);
        if (subscription == null) {
            subscription = new HashSet<>();
        }

        return subscription;
    }

    public static Map<String, Set<String>> getAllSubscriptions() {
        return subscriptions;
    }
}
