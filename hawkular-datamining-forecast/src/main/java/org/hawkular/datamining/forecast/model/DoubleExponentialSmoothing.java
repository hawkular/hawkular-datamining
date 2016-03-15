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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.hawkular.datamining.forecast.AccuracyStatistics;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.Logger;

import com.google.common.collect.EvictingQueue;

/**
 * @author Pavol Loffay
 */
public class DoubleExponentialSmoothing implements TimeSeriesModel {

    public static final double DEFAULT_LEVEL_SMOOTHING = 0.4;
    public static final double DEFAULT_TREND_SMOOTHING = 0.1;
    public static final double MIN_LEVEL_TREND_SMOOTHING = 0.0001;
    public static final double MAX_LEVEL_TREND_SMOOTHING = 0.9999;

    private final double levelSmoothing;
    private final double trendSmoothing;

    public static int MIN_INIT_SIZE = 2;
    private EvictingQueue<DataPoint> initStateWindow;

    private boolean initialized;
    private double level;
    private double slope;

    double sse;
    double absSum;
    long counter;
    private AccuracyStatistics initAccuracy;


    public DoubleExponentialSmoothing() {
        this(DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING);
    }

    /**
     * Double exponential smoothing - additive variant
     */
    public DoubleExponentialSmoothing(double levelSmoothing, double trendSmoothing) {
        if (levelSmoothing < MIN_LEVEL_TREND_SMOOTHING || levelSmoothing > MAX_LEVEL_TREND_SMOOTHING) {
            throw new IllegalArgumentException("Level parameter should be in interval 0-1");
        }
        if (trendSmoothing < MIN_LEVEL_TREND_SMOOTHING || trendSmoothing > MAX_LEVEL_TREND_SMOOTHING) {
            throw new IllegalArgumentException("Trend parameter should be in 0-1");
        }

        this.levelSmoothing = levelSmoothing;
        this.trendSmoothing = trendSmoothing;
        this.initStateWindow = EvictingQueue.create(MIN_INIT_SIZE);
    }

    @Override
    public AccuracyStatistics init(List<DataPoint> dataPoints) {

        initState(dataPoints, false);
        learn(dataPoints);

        initAccuracy = new AccuracyStatistics(sse, sse/(double)dataPoints.size(), absSum/(double)dataPoints.size());
        sse = 0d;
        absSum = 0d;
        counter = 0L;

        return initAccuracy;
    }

    @Override
    public void learn(DataPoint dataPoint) {
        initStateWindow.add(dataPoint);

        if (!initialized) {
            if (initStateWindow.remainingCapacity() == 1) {
                initState(Arrays.asList(dataPoint), true);
            } else {
                return;
            }
        }

        double error = dataPoint.getValue() - forecast().getValue();
        sse += error * error;
        absSum += abs(error);
        counter++;

        double oldLevel = level;
        level = levelSmoothing*dataPoint.getValue() + (1 - levelSmoothing)*(level + slope);
        slope = trendSmoothing*(level - oldLevel) + (1 - trendSmoothing)*(slope);
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {

        if (!initialized && initStateWindow.remainingCapacity() - dataPoints.size() <= 1) {
            // the more points for init state the better
            initState(dataPoints, true);
        }

        dataPoints.forEach(point -> {
            learn(point);
        });
    }

    @Override
    public DataPoint forecast() {
        double prediction = calculatePrediction(1);
        return new DataPoint(prediction, 1L);
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {

        List<DataPoint> result = new ArrayList<>(nAhead);
        for (int i = 1; i <= nAhead; i++) {
            double prediction = calculatePrediction(i);
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
        return new AccuracyStatistics(sse, sse/(double) counter, absSum/(double)counter);
    }

    @Override
    public String name() {
        return "Double exponential smoothing";
    }

    @Override
    public int numberOfParams() {
        return 4;
    }

    private double calculatePrediction(int nAhead) {
        return level + slope * nAhead;
    }

    private void initState(Collection<DataPoint> data, boolean continuous) {

        List<DataPoint> initData = new ArrayList<>(initStateWindow.size() + data.size());
        if (continuous) {
            initData.addAll(initStateWindow);
        }
        initData.addAll(data);

        if (initData.size() < MIN_INIT_SIZE) {
            throw new IllegalArgumentException("For init are required " + MIN_INIT_SIZE + " points.");
        } else {
            if (initData.size() == 2) {
                DataPoint[] dataPoints = initData.toArray(new DataPoint[0]);
                level = dataPoints[0].getValue();
                slope = dataPoints[1].getValue() - dataPoints[0].getValue();
            } else {
                SimpleRegression regression = new SimpleRegression();
                initData.forEach(dataPoint -> regression.addData(dataPoint.getTimestamp(), dataPoint.getValue()));
                level = regression.predict(initData.get(0).getTimestamp());
                slope = regression.getSlope();
            }
        }

        initialized = true;
    }

    public static Optimizer optimizer() {
        return new Optimizer();
    }

    @Override
    public String toString() {
        return "DoubleExponentialSmoothing{" +
                "alpha=" + levelSmoothing +
                ", beta=" + trendSmoothing +
                ", level=" + level +
                ", slope=" + slope +
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
                return new DoubleExponentialSmoothing();
            }

            MultivariateFunctionMappingAdapter constFunction = costFunction(dataPoints);

            int maxIter = 10000;
            int maxEval = 10000;

            // Nelder-Mead Simplex
            SimplexOptimizer nelderSimplexOptimizer = new SimplexOptimizer(0.0001, 0.0001);
            PointValuePair nelderResult = nelderSimplexOptimizer.optimize(
                    GoalType.MINIMIZE, new MaxIter(maxIter), new MaxEval(maxEval),
                    new InitialGuess(new double[]{DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING}),
                    new ObjectiveFunction(constFunction),
                    new NelderMeadSimplex(2));

            result = constFunction.unboundedToBounded(nelderResult.getPoint());
            Logger.LOGGER.debugf("Optimizer best alpha: %.5f, beta %.5f", result[0], result[1]);

            DoubleExponentialSmoothing bestModel = new DoubleExponentialSmoothing(result[0], result[1]);
            bestModel.init(dataPoints);

            return bestModel;

        }

        public MultivariateFunctionMappingAdapter costFunction(final List<DataPoint> dataPoints) {
            // func for minimization
            MultivariateFunction multivariateFunction = point -> {

                double alpha = point[0];
                double beta = point[1];

                if (beta > alpha) {
                    return Double.POSITIVE_INFINITY;
                }

                DoubleExponentialSmoothing doubleExponentialSmoothing = new DoubleExponentialSmoothing(alpha, beta);
                AccuracyStatistics accuracyStatistics = doubleExponentialSmoothing.init(dataPoints);

                Logger.LOGGER.tracef("Optimizer MSE = %f, alpha=%.10f, beta=%.10f", accuracyStatistics.getMse(),
                        alpha, beta);
                return accuracyStatistics.getMse();
            };
            MultivariateFunctionMappingAdapter multivariateFunctionMappingAdapter =
                    new MultivariateFunctionMappingAdapter(multivariateFunction,
                            new double[]{MIN_LEVEL_TREND_SMOOTHING, MIN_LEVEL_TREND_SMOOTHING},
                            new double[]{MAX_LEVEL_TREND_SMOOTHING, MAX_LEVEL_TREND_SMOOTHING});

            return multivariateFunctionMappingAdapter;
        }
    }
}
