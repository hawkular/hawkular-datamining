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

import static java.lang.Math.abs;

import java.util.ArrayList;
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
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.Logger;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;

/**
 * @author Pavol Loffay
 */
public class SimpleExponentialSmoothing implements TimeSeriesModel {

    public static final double DEFAULT_LEVEL_SMOOTHING = 0.4;
    public static final double MIN_LEVEL_SMOOTHING = 0.0001;
    public static final double MAX_LEVEL_SMOOTHING = 0.9999;

    private final double levelSmoothing;

    private boolean initialized;
    private double level;

    double sse;
    double absSum;
    long counter;
    private AccuracyStatistics initAccuracy;


    public SimpleExponentialSmoothing() {
        this(DEFAULT_LEVEL_SMOOTHING);
    }

    public SimpleExponentialSmoothing(double levelSmoothing) {
        if (levelSmoothing < MIN_LEVEL_SMOOTHING || levelSmoothing > MAX_LEVEL_SMOOTHING) {
            throw new IllegalArgumentException("Level parameter should be in interval 0-1");
        }
        this.levelSmoothing = levelSmoothing;
    }

    @Override
    public AccuracyStatistics init(List<DataPoint> dataPoints) {

        initState(dataPoints);
        learn(dataPoints);

        initAccuracy = new AccuracyStatistics(sse, sse/(double)dataPoints.size(), absSum/(double)dataPoints.size());
        sse = 0d;
        absSum = 0d;
        counter = 0L;

        return initAccuracy;
    }

    @Override
    public void learn(DataPoint dataPoint) {

        if (!initialized) {
            initState(Arrays.asList(dataPoint));
        }

        double error = dataPoint.getValue() - forecast().getValue();
        sse += error * error;
        absSum += abs(error);
        counter++;

        level = levelSmoothing*dataPoint.getValue() + (1 - levelSmoothing)*level;
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {

        if (!initialized) {
            // the more points for init state the better
            initState(dataPoints);
        }

        dataPoints.forEach(point -> {
            learn(point);
        });
    }

    @Override
    public DataPoint forecast() {
        double prediction = calculatePrediction();
        return new DataPoint(prediction, 1L);
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {
        double prediction = calculatePrediction();

        List<DataPoint> dataPoints = new ArrayList<>(nAhead);
        for (long i = 1; i <= nAhead; i++) {
            dataPoints.add(new DataPoint(prediction, i));
        }

        return dataPoints;
    }

    @Override
    public AccuracyStatistics initStatistics() {
        return initAccuracy;
    }

    @Override
    public AccuracyStatistics runStatistics() {
        return new AccuracyStatistics(sse, sse/(double)counter, absSum/(double)counter);
    }

    @Override
    public String name() {
        return "Simple exponential smoothing";
    }

    @Override
    public int numberOfParams() {
        return 2;
    }

    private void initState(List<DataPoint> initData) {

        if (initData.isEmpty()) {
            throw new IllegalArgumentException("For init is required at least one point");
        }
        else if (initData.size() == 1) {
            level = initData.get(0).getValue();
        } else {
            // mean
            Double sum = initData.stream().map(dataPoint -> dataPoint.getValue())
                    .reduce((aDouble, aDouble2) -> aDouble + aDouble2).get();
            level = sum / (double) initData.size();
        }

        initialized = true;
    }

    // flat forecast function
    private double calculatePrediction() {
        return level;
    }

    public static Optimizer optimizer() {
        return new Optimizer();
    }

    @Override
    public String toString() {
        return "SimpleExponentialSmoothing{" +
                "alpha=" + levelSmoothing +
                ", level=" + level +
                '}';
    }

    public static class Optimizer implements ModelOptimizer {

        private double[] result;

        @Override
        public double[] result() {
            return result;
        }

        @Override
        public TimeSeriesModel minimizedMSE(List<DataPoint> dataPoints) {
            if (dataPoints.isEmpty()) {
                return new SimpleExponentialSmoothing();
            }

            MultivariateFunctionMappingAdapter constFunction = costFunction(dataPoints);

            int maxIter = 10000;
            int maxEval = 10000;

            // Nelder-Mead Simplex
            SimplexOptimizer nelderSimplexOptimizer = new SimplexOptimizer(0.0001, 0.0001);
            PointValuePair nelderResult = nelderSimplexOptimizer.optimize(
                    GoalType.MINIMIZE, new MaxIter(maxIter), new MaxEval(maxEval),
                    new InitialGuess(new double[]{0.4}), new ObjectiveFunction(constFunction),
                    new NelderMeadSimplex(1));

            result = constFunction.unboundedToBounded(nelderResult.getPoint());
            Logger.LOGGER.debugf("Optimizer best alpha: %f", result[0]);

            SimpleExponentialSmoothing bestModel = new SimpleExponentialSmoothing(result[0]);
            bestModel.init(dataPoints);

            return bestModel;
        }

        public MultivariateFunctionMappingAdapter costFunction(final List<DataPoint> dataPoints) {
            // func for minimization
            MultivariateFunction multivariateFunction = point -> {

                double alpha = point[0];

                SimpleExponentialSmoothing doubleExponentialSmoothing = new SimpleExponentialSmoothing(alpha);
                AccuracyStatistics accuracyStatistics = doubleExponentialSmoothing.init(dataPoints);

                Logger.LOGGER.tracef("Optimizer MSE = %f, alpha=%.10f", accuracyStatistics.getMse(), alpha);
                return accuracyStatistics.getMse();
            };
            MultivariateFunctionMappingAdapter multivariateFunctionMappingAdapter =
                    new MultivariateFunctionMappingAdapter(multivariateFunction,
                            new double[]{MIN_LEVEL_SMOOTHING}, new double[]{MAX_LEVEL_SMOOTHING});

            return multivariateFunctionMappingAdapter;
        }
    }
}
