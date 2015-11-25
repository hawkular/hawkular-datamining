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

package org.hawkular.datamining.engine;

import java.util.List;

import org.hawkular.datamining.api.EngineDataReceiver;
import org.hawkular.datamining.api.ModelSubscription;
import org.hawkular.datamining.api.TimeSeriesLinkedModel;
import org.hawkular.datamining.api.model.DataPoint;
import org.hawkular.datamining.api.model.MetricData;
import org.hawkular.inventory.api.model.Metric;

/**
 * @author Pavol Loffay
 */
public class ForecastingEngine implements EngineDataReceiver<MetricData>,
        org.hawkular.datamining.api.ForecastingEngine {

    private final MetricsStorageAdapter metricsStorageAdapter = new MetricsStorageAdapter();
    private final InventoryStorageAdapter inventoryStorageAdapter = new InventoryStorageAdapter();

    private final ModelSubscription subscriptionManager;


    public ForecastingEngine(ModelSubscription subscriptionManager) {
        this.subscriptionManager = subscriptionManager;

        initializeAll();
    }

    @Override
    public void process(MetricData metricData) {
        if (!subscriptionManager.subscribes(metricData.getTenant(), metricData.getMetricId())) {
            return;
        }

        EngineLogger.LOGGER.debugf("Process %s, %s", metricData.getTenant(), metricData.getMetricId());

        TimeSeriesLinkedModel model=  subscriptionManager.getModel(metricData.getTenant(), metricData.getMetricId());
        model.addDataPoint(metricData.getDataPoint());
    }

    @Override
    public void process(List<MetricData> data) {
        data.forEach(x -> process(x));
    }

    @Override
    public List<DataPoint> predict(String tenant, String metricsId, int nAhead) {

        TimeSeriesLinkedModel model = subscriptionManager.getModel(tenant, metricsId);
        List<DataPoint> points = model.predict(nAhead);

        return points;
    }

    private void initializeAll() {
        List<TimeSeriesLinkedModel> allModels = subscriptionManager.getAllModels();

        allModels.forEach(x -> initializeModel(x));
    }

    private void initializeModel(TimeSeriesLinkedModel model) {

        org.hawkular.datamining.api.model.Metric metric = model.getLinkedMetric();

        List<DataPoint> dataPoints = metricsStorageAdapter.loadPoints(metric.getId(), metric.getTenant());
        Metric metricDefinition = inventoryStorageAdapter.getMetricDefinition(metric.getId(), metric.getFeed(),
                metric.getTenant());

        Long inventoryInterval = metricDefinition.getCollectionInterval() == null ?
                metricDefinition.getType().getCollectionInterval() : metricDefinition.getCollectionInterval();

        Long interval = metric.getInterval() == null ? inventoryInterval : metric.getInterval();

        model.setInterval(interval);
        model.addDataPoints(dataPoints);
    }
}
