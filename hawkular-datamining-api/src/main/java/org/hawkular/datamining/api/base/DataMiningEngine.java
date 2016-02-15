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
import java.util.List;
import java.util.Set;

import org.hawkular.datamining.api.Subscription;
import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.exception.SubscriptionNotFoundException;
import org.hawkular.datamining.api.model.MetricData;
import org.hawkular.datamining.api.storage.PredictionListener;
import org.hawkular.datamining.forecast.DataPoint;

/**
 * @author Pavol Loffay
 */
public class DataMiningEngine implements org.hawkular.datamining.api.DataMiningEngine<MetricData> {

    private final SubscriptionManager subscriptionManager;

    private Set<PredictionListener> predictionListeners;

    public DataMiningEngine(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
        this.predictionListeners = new HashSet<>();
    }

    @Override
    public void process(MetricData metricData) {
        if (!subscriptionManager.subscribes(metricData.getTenant(), metricData.getMetricId())) {
            throw new SubscriptionNotFoundException(metricData.getTenant(), metricData.getMetricId());
        }

        Subscription subscription = subscriptionManager.subscription(metricData.getTenant(), metricData.getMetricId());
        subscription.forecaster().learn(metricData.getDataPoint());

        if (subscription.getForecastingHorizon() == null || subscription.getForecastingHorizon() == 0) {
            return;
        }
        int nAhead = (int) (subscription.getForecastingHorizon() / subscription.getCollectionInterval()) + 1;

        List<DataPoint> predicted = predict(metricData.getTenant(), metricData.getMetricId(), nAhead);
        predictionListeners.forEach(listener -> listener.send(predicted, metricData.getTenant(),
                metricData.getMetricId()));
    }

    @Override
    public void process(List<MetricData> data) {
        data.forEach(x -> process(x));
    }

    @Override
    public List<DataPoint> predict(String tenant, String metricsId, int nAhead) {

        Subscription subscription = subscriptionManager.subscription(tenant, metricsId);
        List<DataPoint> points = subscription.forecaster().forecast(nAhead);

        return points;
    }

    @Override
    public void addPredictionListener(PredictionListener predictionListener) {
        predictionListeners.add(predictionListener);
    }
}
