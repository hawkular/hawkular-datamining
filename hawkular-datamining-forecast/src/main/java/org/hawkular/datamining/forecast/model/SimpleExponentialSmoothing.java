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

package org.hawkular.datamining.forecast.model;

import static java.lang.Math.abs;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateFunctionMappingAdapter;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.hawkular.datamining.forecast.AccuracyStatistics;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.Logger;

/**
 * @author Pavol Loffay
 */
public class SimpleExponentialSmoothing implements TimeSeriesModel {

    public static final double DEFAULT_LEVEL_SMOOTHING = 0.4;
    public static final int MIN_BUFFER_SIZE = 5;

    private final double levelSmoothing;

    private double level;

    private AccuracyStatistics initAccuracy;


    public SimpleExponentialSmoothing() {
        this(DEFAULT_LEVEL_SMOOTHING);
    }

    public SimpleExponentialSmoothing(double levelSmoothing) {
        if (levelSmoothing < 0.0 || levelSmoothing > 1.0) {
            throw new IllegalArgumentException("Level parameter should be in interval 0-1");
        }
        this.levelSmoothing = levelSmoothing;
    }

    @Override
    public void learn(DataPoint dataPoint) {
        learn(Arrays.asList(dataPoint));
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {
        dataPoints.forEach(point -> {
            level = levelSmoothing * point.getValue() + (1 - levelSmoothing) * level;
        });
    }

    @Override
    public DataPoint forecast() {
        double prediction = calculatePrediction();
        return new DataPoint(prediction, 0L);
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {
        return null;
    }

    public AccuracyStatistics init(List<DataPoint> dataPoints) {

        if (dataPoints == null || dataPoints.size() < MIN_BUFFER_SIZE) {
            throw new IllegalArgumentException("For init are required " + MIN_BUFFER_SIZE + " points.");
        }

        level = 0;

        double mseSum = 0;
        double maeSum = 0;

        for (DataPoint point: dataPoints) {

            learn(point);
            double error = forecast().getValue() - point.getValue();

            mseSum += error * error;
            maeSum += abs(error);
        }

        initAccuracy = new AccuracyStatistics(mseSum/ (double) dataPoints.size(),
                maeSum / (double) dataPoints.size());

        return initAccuracy;
    }

    @Override
    public AccuracyStatistics statistics() {
        return initAccuracy;
    }

    // flat forecast function
    private double calculatePrediction() {
        return level;
    }

    public static class Optimizer implements ModelOptimization {

        @Override
        public TimeSeriesModel minimizedMSE(List<DataPoint> dataPoints) {

            MultivariateFunctionMappingAdapter constFunction = costFunction(dataPoints);

            int maxIter = 10000;
            int maxEval = 10000;

            // Nelder-Mead Simplex
            SimplexOptimizer nelderSimplexOptimizer = new SimplexOptimizer(0.0001, 0.0001);
            PointValuePair nelderResult = nelderSimplexOptimizer.optimize(
                    GoalType.MINIMIZE, new MaxIter(maxIter), new MaxEval(maxEval),
                    new InitialGuess(new double[]{0.5}), new ObjectiveFunction(constFunction),
                    new NelderMeadSimplex(1));

            double[] param = constFunction.unboundedToBounded(nelderResult.getPoint());

            SimpleExponentialSmoothing bestModel = new SimpleExponentialSmoothing(param[0]);
            bestModel.init(dataPoints);

            return bestModel;
        }

        private MultivariateFunctionMappingAdapter costFunction(final List<DataPoint> dataPoints) {
            // func for minimization
            MultivariateFunction multivariateFunction = point -> {

                double alpha = point[0];

                SimpleExponentialSmoothing doubleExponentialSmoothing = new SimpleExponentialSmoothing(alpha);
                AccuracyStatistics accuracyStatistics = doubleExponentialSmoothing.init(dataPoints);

                Logger.LOGGER.tracef("%s MSE = %s, alpha=%f, beta=%f\n",
                        accuracyStatistics.getMse(), alpha);
                return accuracyStatistics.getMse();
            };
            MultivariateFunctionMappingAdapter multivariateFunctionMappingAdapter =
                    new MultivariateFunctionMappingAdapter(multivariateFunction,
                            new double[]{0.0}, new double[]{1});

            return multivariateFunctionMappingAdapter;
        }
    }
}
