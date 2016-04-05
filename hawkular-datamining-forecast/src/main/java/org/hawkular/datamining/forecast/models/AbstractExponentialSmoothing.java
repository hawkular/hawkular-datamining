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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.MetricContext;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;

/**
 * @author Pavol Loffay
 */
public abstract class AbstractExponentialSmoothing implements TimeSeriesModel {

    private static Comparator<DataPoint> dataPointComparator = new TimestampComparator();

    // in ms
    protected long lastTimestamp = -1;
    protected MetricContext metricContext;

    private long counter;
    private double sse;
    private double absSum;
    private AccuracyStatistics initAccuracy;

    protected abstract double calculatePrediction(long nAhead, Long learnTimestamp);
    protected abstract void updateState(DataPoint dataPoint);
    protected abstract SimpleExponentialSmoothing.State initState(List<DataPoint> dataPoints);
    protected abstract SimpleExponentialSmoothing.State state();


    public AbstractExponentialSmoothing(MetricContext metricContext) {
        this.metricContext = metricContext;
    }

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

        if (dataPoint.getTimestamp() <= lastTimestamp) {
            throw new IllegalArgumentException("Data point has older timestamp than current state.");
        }

        double error = dataPoint.getValue() - calculatePrediction(1, dataPoint.getTimestamp());
        sse += error * error;
        absSum += Math.abs(error);
        counter++;

        lastTimestamp = dataPoint.getTimestamp();
        updateState(dataPoint);
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {

        Collections.sort(dataPoints, dataPointComparator);

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
        return new DataPoint(prediction, lastTimestamp + metricContext.getCollectionInterval());
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {

        List<DataPoint> result = new ArrayList<>(nAhead);
        for (long i = 1; i <= nAhead; i++) {

            DataPoint predictedPoint = new DataPoint(calculatePrediction(i, null),
                    lastTimestamp + i*metricContext.getCollectionInterval());

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

    @Override
    public long lastTimestamp() {
        return lastTimestamp;
    }
}
