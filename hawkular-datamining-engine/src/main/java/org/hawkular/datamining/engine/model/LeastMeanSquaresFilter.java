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
import java.util.Collection;
import java.util.List;

import org.hawkular.datamining.api.model.DataPoint;

/**
 * @author Pavol Loffay
 */
public class LeastMeanSquaresFilter implements PredictionModel {

    private final double alphaLearningRate;
    private final int filterLength;
    private double[] weights;
    private double[] oldPoints;

    private int initialized = 0;

    public LeastMeanSquaresFilter(double alphaLearningRate, double[] weights) {
        this.alphaLearningRate = alphaLearningRate;
        this.weights = weights;
        this.filterLength = weights.length;

        this.oldPoints = new double[filterLength];
    }

    public double[] getWeights() {
        return Arrays.copyOf(weights, weights.length);
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
    public List<DataPoint> predict(int nAhead) {
        LeastMeanSquaresFilter lmsPredict = new LeastMeanSquaresFilter(this.alphaLearningRate, this.weights);

        List<DataPoint> result = new ArrayList<>(nAhead);
        for (int i = 0; i < nAhead; i++) {
            DataPoint predictedPoint = lmsPredict.predict();
            result.add(predictedPoint);

            lmsPredict.process(Arrays.asList(predictedPoint));
        }

        return result;
    }

    @Override
    public  DataPoint predict() {
        double prediction = currentPrediction();

        return new DataPoint(prediction, 1L);
    }

    private void process(Collection<DataPoint> dataPoints) {

        for (DataPoint dataPoint: dataPoints) {
            if (initialized++ <= filterLength) {
                updateFilterPoints(dataPoint.getValue());
                continue;
            }

            double currentPrediction = currentPrediction();
            double error = (dataPoint.getValue() - (currentPrediction));

            // update weights
            for (int i = 0; i < filterLength; i++) {
                weights[i] = weights[i] - (alphaLearningRate * error * oldPoints[i]);
//                weights[i] = weights[i] - (error * oldPoints[i]) / (oldPoints[i] * oldPoints[i]);
            }

            updateFilterPoints(dataPoint.getValue());
        }
    }

    private double currentPrediction() {
        double oldPrediction = 0;
        for (int i = 0; i < filterLength; i++) {
                oldPrediction  += (-weights[i]) * oldPoints[i];
        }

        return oldPrediction;
    }

    private void updateFilterPoints(double point) {
        for(int i = filterLength - 1; i > 0; i--) {
            oldPoints[i] = oldPoints[i - 1];
        }

        oldPoints[0] = point;
    }
}
