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

package org.hawkular.datamining.engine;

import java.util.List;

import org.hawkular.datamining.api.SubscriptionManager;
import org.hawkular.datamining.api.TimeSeriesLinkedModel;
import org.hawkular.datamining.api.model.DataPoint;
import org.hawkular.datamining.api.model.MetricData;
import org.hawkular.datamining.api.storage.PredictionStorage;
import org.hawkular.datamining.bus.BusConfiguration;
import org.hawkular.datamining.bus.sender.PredictionSender;

/**
 * @author Pavol Loffay
 */
public class ForecastingEngine implements org.hawkular.datamining.api.ForecastingEngine<MetricData> {

    private final SubscriptionManager subscriptionManager;

    private PredictionStorage predictionOutput;

    public ForecastingEngine(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;

        // todo should be CDI
        this.predictionOutput = new PredictionSender(BusConfiguration.TOPIC_METRIC_DATA, BusConfiguration.BROKER_URL);
    }

    @Override
    public void process(MetricData metricData) {
        if (!subscriptionManager.subscribes(metricData.getTenant(), metricData.getMetricId())) {
            return;
        }

        TimeSeriesLinkedModel model = subscriptionManager.model(metricData.getTenant(), metricData.getMetricId());
        model.addDataPoint(metricData.getDataPoint());

        if (model.getPredictionInterval() == null || model.getPredictionInterval() == 0) {
            return;
        }
        int nAhead = (int) (model.getPredictionInterval() / model.getCollectionInterval()) + 1;

        List<DataPoint> predicted = predict(metricData.getTenant(), metricData.getMetricId(), nAhead);
        predictionOutput.send(predicted, metricData.getTenant(), metricData.getMetricId());
    }

    @Override
    public void process(List<MetricData> data) {
        data.forEach(x -> process(x));
    }

    @Override
    public List<DataPoint> predict(String tenant, String metricsId, int nAhead) {

        TimeSeriesLinkedModel model = subscriptionManager.model(tenant, metricsId);
        List<DataPoint> points = model.predict(nAhead);

        return points;
    }
}
