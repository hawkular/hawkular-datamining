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

import java.util.List;

import org.hawkular.datamining.api.PredictionListener;
import org.hawkular.datamining.forecast.AutomaticForecaster;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.MetricContext;

/**
 * @author Pavol Loffay
 */
public class DataMiningForecaster extends AutomaticForecaster {

    private Long forecastingHorizon;
    private PredictionListener predictionListener;


    public DataMiningForecaster(MetricContext context) {
        this(context, null);
    }

    public DataMiningForecaster(MetricContext context, Long forecastingHorizon) {
        super(context);
        this.forecastingHorizon = forecastingHorizon;
    }

    @Override
    public void learn(DataPoint dataPoint) {
        super.learn(dataPoint);

        automaticPrediction();
    }

    @Override
    public void learn(List<DataPoint> data) {
        super.learn(data);

        automaticPrediction();
    }

    public void setPredictionListener(PredictionListener predictionListener) {
        this.predictionListener = predictionListener;
    }

    public Long getForecastingHorizon() {
        return forecastingHorizon;
    }

    public void setForecastingHorizon(Long forecastingHorizon) {
        this.forecastingHorizon = forecastingHorizon;
    }

    private void automaticPrediction() {
        if (initialized()) {
            if (predictionListener != null && forecastingHorizon != null) {
                int nAhead = (int) (forecastingHorizon / context().getCollectionInterval()) + 1;
                List<DataPoint> prediction = forecast(nAhead);

                predictionListener.send(prediction, context().getTenant(), context().getMetricId());
            }
        }
    }
}
