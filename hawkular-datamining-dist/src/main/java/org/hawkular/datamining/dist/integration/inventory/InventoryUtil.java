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
package org.hawkular.datamining.dist.integration.inventory;

import java.util.HashSet;
import java.util.Set;

import org.hawkular.datamining.api.Subscription;
import org.hawkular.datamining.api.model.MetricDataType;
import org.hawkular.datamining.dist.integration.Configuration;
import org.hawkular.inventory.api.model.CanonicalPath;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricType;
import org.hawkular.inventory.api.model.Relationship;
import org.hawkular.inventory.api.model.Tenant;

/**
 * @author Pavol Loffay
 */
public class InventoryUtil {

    private InventoryUtil() {
    }

    public static org.hawkular.datamining.api.model.MetricType convertMetricType(MetricType type) {
        MetricDataType dataType = convertDataType(type.getType());

        return new org.hawkular.datamining.api.model.MetricType(type.getPath().ids().getTenantId(),
                type.getCollectionInterval(), dataType);
    }

    public static org.hawkular.datamining.api.model.Metric convertMetric(Metric metric, Long forecastingHorizon) {
        org.hawkular.datamining.api.model.MetricType type = convertMetricType(metric.getType());

        return new org.hawkular.datamining.api.model.Metric(metric.getPath().ids().getTenantId(),
                metric.getPath().ids().getFeedId(), metric.getId(), metric.getCollectionInterval(),
                forecastingHorizon, type);
    }

    public static Long parseForecastingHorizon(Relationship relationship) {
        if (relationship == null) {
            return 0L;
        }

        String forecastingHorizonObject = (String) relationship.getProperties()
                .get(Configuration.PREDICTION_INTERVAL_PROP);

        return forecastingHorizonObject == null ? 0L : Long.parseLong(forecastingHorizonObject);
    }

    public static Set<Subscription.SubscriptionOwner> predictionRelationshipsToOwners(Set<Relationship>
                                                                                              relationships) {
        Set<Subscription.SubscriptionOwner> subscriptionOwners = new HashSet<>();

        for (Relationship relationship: relationships) {
            Class<?> targetEntity = relationship.getTarget().getSegment().getElementType();

            if (targetEntity.equals(Metric.class)) {
                subscriptionOwners.add(Subscription.SubscriptionOwner.Metric);
            } else if (targetEntity.equals(MetricType.class)) {
                subscriptionOwners.add(Subscription.SubscriptionOwner.MetricType);
            } else if (targetEntity.equals(Tenant.class)) {
                subscriptionOwners.add(Subscription.SubscriptionOwner.Tenant);
            }
        }

        return subscriptionOwners;
    }

    public static Subscription.SubscriptionOwner parseSubscriptionOwner(CanonicalPath canonicalPath) {
        Class<?> entity = canonicalPath.getSegment().getElementType();
        if (entity.equals(Tenant.class)) {
            return Subscription.SubscriptionOwner.Tenant;
        } else if (entity.equals(MetricType.class)) {
            return Subscription.SubscriptionOwner.MetricType;
        } else if (entity.equals(Metric.class)) {
            return Subscription.SubscriptionOwner.Metric;
        }

        return null;
    }

    public static Long closestForecastingHorizon(Set<Relationship> relationships) {
        Relationship closestRelationship = null;

        for (Relationship rel: relationships) {
            Class<?> target = rel.getTarget().getSegment().getElementType();

            if (target.equals(Metric.class)) {
                closestRelationship = rel;
            } else if (target.equals(MetricType.class) &&(closestRelationship == null ||
                    closestRelationship.getTarget().getSegment().getElementType().equals(Tenant.class))) {

                closestRelationship = rel;
            } else if (target.equals(Tenant.class) && closestRelationship == null) {
                closestRelationship = rel;
            }
        }

        if (closestRelationship == null) {
            return 0L;
        }
        return InventoryUtil.parseForecastingHorizon(closestRelationship);
    }

    public static MetricDataType convertDataType(org.hawkular.inventory.api.model.MetricDataType dataType) {

        MetricDataType dataMiningType = null;

        switch (dataType) {
            case AVAILABILITY: dataMiningType = MetricDataType.AVAILABILITY;
                break;
            case COUNTER: dataMiningType = MetricDataType.COUNTER;
                break;
            case COUNTER_RATE: dataMiningType = MetricDataType.COUNTER_RATE;
                break;

            default:
            case GAUGE: dataMiningType = MetricDataType.GAUGE;
                break;
        }

        return dataMiningType;
    }
}
