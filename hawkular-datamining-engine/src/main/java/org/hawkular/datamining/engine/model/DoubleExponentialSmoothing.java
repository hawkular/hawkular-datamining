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
package org.hawkular.datamining.engine.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.hawkular.datamining.api.TimeSeriesModel;
import org.hawkular.datamining.api.model.DataPoint;

import com.google.common.collect.EvictingQueue;

/**
 * @author Pavol Loffay
 */
public class DoubleExponentialSmoothing implements TimeSeriesModel {

    public static int BUFFER_SIZE = 5;

    private double levelSmoothing;
    private double trendSmoothing;

    private double level;
    private double slope;

    private EvictingQueue<DataPoint> oldPoints;


    public DoubleExponentialSmoothing(double levelSmoothing, double trendSmoothing) {
        this.levelSmoothing = levelSmoothing;
        this.trendSmoothing = trendSmoothing;
        this.oldPoints = EvictingQueue.create(BUFFER_SIZE);
    }

    @Override
    public void learn(DataPoint dataPoint) {
        learn(Arrays.asList(dataPoint));
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {
        for (DataPoint point: dataPoints) {

            oldPoints.add(point);

            if (oldPoints.remainingCapacity() == 1) {
                // compute level, trend
                level = oldPoints.element().getValue();
                slope = getInitialSlope(oldPoints.iterator());
                oldPoints.forEach(oldPoint -> updateLevelAndSlope(oldPoint));
                continue;
            }

            if (oldPoints.remainingCapacity() > 0) {
                continue;
            }

            updateLevelAndSlope(point);
        }
    }

    @Override
    public DataPoint predict() {
        double prediction = calculatePrediction(1);
        return new DataPoint(prediction, 1L);
    }

    @Override
    public List<DataPoint> predict(int nAhead) {

        List<DataPoint> result = new ArrayList<>(nAhead);
        for (int i = 0; i < nAhead; i++) {
            double prediction = calculatePrediction(i);
            DataPoint predictedPoint = new DataPoint(prediction,(long) i);

            result.add(predictedPoint);
        }

        return result;
    }

    private void updateLevelAndSlope(DataPoint point) {
        double level_old = level;
        level = levelSmoothing * point.getValue() + (1 - levelSmoothing) * (level + slope);
        slope = trendSmoothing * (level - level_old) + (1 - trendSmoothing) * (slope);
    }

    private double calculatePrediction(int nAhead) {
        return level + slope * nAhead;
    }

    public double getInitialSlope(Iterator<DataPoint> dataPointStream) {
        double slope = 0;
        int count = 0;

        if (!dataPointStream.hasNext()) {
            return slope;
        }

        DataPoint previous = dataPointStream.next();
        while (dataPointStream.hasNext()) {
            DataPoint current = dataPointStream.next();

            slope += current.getValue() - previous.getValue();
            count++;
        }

        return slope / count;
    }
}
