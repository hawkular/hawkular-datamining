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
import java.util.List;

import org.hawkular.datamining.api.model.DataPoint;

/**
 * @author Pavol Loffay
 */
public class ExponentiallyWeightedMovingAverages implements PredictionModel {

    private double levelSmoothing;
    private double trendSmoothing;

    private double level;
    private double slope;


    public ExponentiallyWeightedMovingAverages(double levelSmoothing, double trendSmoothing) {
        this.levelSmoothing = levelSmoothing;
        this.trendSmoothing = trendSmoothing;
    }

    @Override
    public void addDataPoint(DataPoint dataPoint) {
        process(Arrays.asList(dataPoint));
    }

    @Override
    public void addDataPoints(List<DataPoint> dataPoints) {
        process(dataPoints);
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
            double prediction = calculatePrediction(nAhead);
            DataPoint predictedPoint = new DataPoint(prediction,(long) i);

            result.add(predictedPoint);
        }

        return result;
    }

    private void process(List<DataPoint> dataPoints) {

        for (DataPoint point: dataPoints) {
            double level_old = level;
            level = levelSmoothing * point.getValue() + (1 - levelSmoothing) * (level + slope);
            slope = trendSmoothing * (level - level_old) + (1 - trendSmoothing) * (slope);
        }
    }

    private double calculatePrediction(int nAhead) {
        return level + nAhead * slope;
    }
}
