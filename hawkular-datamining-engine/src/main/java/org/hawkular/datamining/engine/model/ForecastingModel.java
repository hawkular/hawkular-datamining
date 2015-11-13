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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hawkular.datamining.api.model.DataPoint;
import org.hawkular.datamining.engine.EngineLogger;

/**
 * @author Pavol Loffay
 */
public class ForecastingModel implements PredictionModel {
    // ewma
    public static final double EWMA_ALPHA = 0.005;
    public static final double EWMA_BETA = 0.005;
    // lms
    public static final double[] LMS_WEIGHTS = new double[] {3, -1};
    // TODO this has to be calculated from data, or use normalized version
    public static final double LMS_ALPHA = 0.000000000000000000000000001;

    private String tenant;
    private String metricId;

    private long lastTimestamp;
    private long distance;

    private LeastMeanSquaresFilter leastMeanSquaresFilter;
    private ExponentiallyWeightedMovingAverages ewma;


    public ForecastingModel(String tenant, String metricId) {
        this.tenant = tenant;
        this.metricId = metricId;

        this.ewma = new ExponentiallyWeightedMovingAverages(EWMA_ALPHA, EWMA_BETA);
        this.leastMeanSquaresFilter = new LeastMeanSquaresFilter(LMS_ALPHA, LMS_WEIGHTS);
    }

    @Override
    public void addDataPoint(DataPoint dataPoint) {
        addData(Arrays.asList(dataPoint));
    }

    @Override
    public void addDataPoints(List<DataPoint> dataPoints) {
        addData(dataPoints);
    }

    @Override
    public List<DataPoint> predict(int nAhead) {

        List<DataPoint> result = new ArrayList<>();
        List<DataPoint> predictionEWMA = ewma.predict(nAhead);
        List<DataPoint> predictionFilter = leastMeanSquaresFilter.predict(nAhead);

        for (int i = 0; i < predictionEWMA.size() && i  < predictionFilter.size(); i++) {

            double ewma = predictionEWMA.get(i).getValue();
            double filter = predictionFilter.get(i).getValue();

            EngineLogger.LOGGER.debugf("Filter predicted %f", filter);
            EngineLogger.LOGGER.debugf("EWMA predicted %f", ewma);

            double mean = (ewma + filter) / 2;
            ewma = ewma - mean;
            filter = filter - mean;

            DataPoint dataPoint = new DataPoint((ewma + filter) + mean, null);
            result.add(dataPoint);
            EngineLogger.LOGGER.debugf("Prediction: %s, %s", tenant, metricId, dataPoint);
        }

        return result;
    }

    public String getTenant() {
        return tenant;
    }

    public String getMetricId() {
        return metricId;
    }

    private void addData(List<DataPoint> dataPoints) {
        // todo data from metrics arrives at <new>, <older>. Maybe its better to reverse itterate
        Collections.sort(dataPoints);

        long newLastTimestamp = dataPoints.get(dataPoints.size() - 1).getTimestamp();
        if (newLastTimestamp < lastTimestamp) {
            throw new IllegalArgumentException("Data point has older timestamp than current state.");
        }

        lastTimestamp = newLastTimestamp;

        ewma.addDataPoints(dataPoints);
        leastMeanSquaresFilter.addDataPoints(dataPoints);
    }
}
