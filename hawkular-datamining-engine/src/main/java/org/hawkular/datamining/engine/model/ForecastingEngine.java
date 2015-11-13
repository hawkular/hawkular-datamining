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

package org.hawkular.datamining.engine.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.hawkular.datamining.api.EngineDataReceiver;
import org.hawkular.datamining.api.MetricFilter;
import org.hawkular.datamining.api.model.DataPoint;
import org.hawkular.datamining.api.model.MetricData;
import org.hawkular.datamining.engine.BatchMetricsLoader;

/**
 * @author Pavol Loffay
 */
@Singleton
public class ForecastingEngine implements EngineDataReceiver<MetricData>,
        org.hawkular.datamining.api.ForecastingEngine {

    //tenant, metric, model, TODO synchronize
    private Map<String, Map<String, ForecastingModel>> models = new HashMap<>();


    public ForecastingEngine() {
        // list data from filter and initialize model
    }

    @Override
    public void process(MetricData metricData) {
        if (!MetricFilter.contains(metricData.getTenant(), metricData.getMetricId())) {
            return;
        }

        ForecastingModel forecastingModel = getForecastingModel(metricData.getTenant(), metricData.getMetricId());
        forecastingModel.addDataPoint(metricData.getDataPoint());
    }

    @Override
    public void process(List<MetricData> data) {
        // TODO
        throw new UnsupportedOperationException();

    }

    @Override
    public List<DataPoint> predict(String tenant, String metricsId, int nAhead) {

        ForecastingModel forecastingModel = getForecastingModel(tenant, metricsId);
        List<DataPoint> points = forecastingModel.predict(nAhead);

        return points;
    }

    private synchronized ForecastingModel getForecastingModel(String tenant, String metricsId) {
        Map<String, ForecastingModel> metricsModels = models.get(tenant);
        if (metricsModels == null) {
            metricsModels = new HashMap<>();

            models.put(tenant, metricsModels);
        }

        ForecastingModel forecastingModel = metricsModels.get(metricsId);
        if (forecastingModel == null) {
            forecastingModel = new ForecastingModel(tenant, metricsId);

            initializeModel(forecastingModel);

            metricsModels.put(metricsId, forecastingModel);
        }

        return forecastingModel;
    }

    private void initializeModel(ForecastingModel model) {

        BatchMetricsLoader batchLoader = new BatchMetricsLoader(model.getTenant(), model.getMetricId());

        List<DataPoint> dataPoints = batchLoader.loadPoints();

        model.addDataPoints(dataPoints);
    }
}
