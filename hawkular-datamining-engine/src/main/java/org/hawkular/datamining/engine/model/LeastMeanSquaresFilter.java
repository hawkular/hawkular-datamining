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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.hawkular.dataminig.api.model.DataPoint;

/**
 * @author Pavol Loffay
 */
public class LeastMeanSquaresFilter implements PredictionModel {

    private final double alpha;
    private final int filterLength;
    private double[] weights;
    private double[] oldPoints;

    private int initialized = 0;

    public LeastMeanSquaresFilter(double alpha, double[] weights) {
        this.alpha = alpha;
        this.weights = weights;
        this.filterLength = weights.length;

        this.oldPoints = new double[filterLength];
    }


    @Override
    public void addDataPoint(DataPoint dataPoint) {
        process(Arrays.asList(dataPoint));
    }

    @Override
    public void addDataPoints(Collection<DataPoint> dataPoints) {
        process(dataPoints);
    }

    @Override
    public List<DataPoint> predict(int nAhead) {

        double value = currentFilterPrediction();
        DataPoint dataPoint = new DataPoint(value, null);
        return Arrays.asList(dataPoint);
    }

    private void process(Collection<DataPoint> dataPoints) {

        for (DataPoint dataPoint: dataPoints) {
            if (initialized++ <= filterLength) {
                updateFilterPoints(dataPoint.getValue());
                continue;
            }

            double currentPrediction = currentFilterPrediction();
            double error = (dataPoint.getValue() - (currentPrediction));

            // update weights
            for (int i = 0; i < filterLength; i++) {
                weights[i] = weights[i] - alpha * error * oldPoints[i];
            }

            updateFilterPoints(dataPoint.getValue());
        }
    }

    private double currentFilterPrediction() {
        double oldPrediction = 0;
        for (int i = 0; i < filterLength; i++) {
            oldPrediction += (-weights[i]) * oldPoints[i];
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
