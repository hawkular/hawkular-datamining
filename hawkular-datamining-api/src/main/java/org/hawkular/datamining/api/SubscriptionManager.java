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

import java.util.Set;

import org.hawkular.datamining.api.model.Metric;
import org.hawkular.datamining.forecast.AutomaticForecaster;
import org.hawkular.datamining.forecast.models.TimeSeriesModel;
import org.hawkular.datamining.forecast.stats.InformationCriterion;

/**
 * SubscriptionManager provides access to all subscriptions in Data Mining.
 *
 * @author Pavol Loffay
 */
public interface SubscriptionManager {

    void subscribe(Subscription subscription);

    void updateMetric(String tenant, String metricId, Metric.Update update);
    void updateForecaster(String tenant, String metricId, org.hawkular.datamining.forecast.Forecaster.Update update);

    boolean isSubscribed(String tenant, String metricId);
    Subscription subscription(String tenant, String metricId);
    Set<Subscription> subscriptionsOfTenant(String tenant);

    void unsubscribeAll(String tenant, String metricId);
    void unsubscribe(String tenant, String metricId, Subscription.SubscriptionOwner subscriptionOwner);
    void unsubscribe(String tenant, String metricId, Set<Subscription.SubscriptionOwner> subscriptionOwners);

    void setPredictionListener(PredictionListener predictionListener);

    class Forecaster {
        private int windowsSize;
        private Class<? extends TimeSeriesModel> recommendedModel;
        private InformationCriterion informationCriterionForModelSelecting;
        private AutomaticForecaster.ConceptDriftStrategy conceptDriftStrategy;
    }
}
