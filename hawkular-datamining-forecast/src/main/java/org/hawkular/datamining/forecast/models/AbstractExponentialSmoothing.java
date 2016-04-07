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

package org.hawkular.datamining.forecast.models;

import java.util.ArrayList;
import java.util.List;

import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;

/**
 * @author Pavol Loffay
 */
public abstract class AbstractExponentialSmoothing implements TimeSeriesModel {

    private long counter;
    private double sse;
    private double absSum;
    private AccuracyStatistics initAccuracy;


    protected abstract double calculatePrediction(int nAhead, DataPoint learnDataPoint);
    protected abstract void updateState(DataPoint dataPoint);
    protected abstract SimpleExponentialSmoothing.State initState(List<DataPoint> dataPoints);

    protected abstract SimpleExponentialSmoothing.State state();

    @Override
    public AccuracyStatistics init(List<DataPoint> dataPoints) {

        if (state() == null) {
            initState(dataPoints);
        }

        dataPoints.forEach(dataPoint -> learn(dataPoint));

        initAccuracy = new AccuracyStatistics(sse, sse/dataPoints.size(), absSum/dataPoints.size());
        counter = 0L;
        sse = 0d;
        absSum = 0d;

        return initAccuracy;
    }

    @Override
    public void learn(DataPoint dataPoint) {

        double error = dataPoint.getValue() - calculatePrediction(1, dataPoint);
        sse += error * error;
        absSum += Math.abs(error);
        counter++;

        updateState(dataPoint);
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {
        if (initAccuracy == null && dataPoints.size() >= minimumInitSize()) {
            AccuracyStatistics init = init(dataPoints);
            sse = init.getSse();
            absSum = init.getMae()*dataPoints.size();
            counter = dataPoints.size();
            return;
        }

        dataPoints.forEach(dataPoint -> learn(dataPoint));
    }

    @Override
    public DataPoint forecast() {
        double prediction = calculatePrediction(1, null);
        return new DataPoint(prediction, 1L);
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {

        List<DataPoint> result = new ArrayList<>(nAhead);
        for (int i = 1; i <= nAhead; i++) {
            double prediction = calculatePrediction(i, null);
            DataPoint predictedPoint = new DataPoint(prediction,(long) i);

            result.add(predictedPoint);
        }

        return result;
    }

    @Override
    public AccuracyStatistics initStatistics() {
        return initAccuracy;
    }

    @Override
    public AccuracyStatistics runStatistics() {
        return new AccuracyStatistics(sse, sse/counter, absSum/counter);
    }
}
