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

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
import org.hawkular.datamining.api.AccuracyStatistics;
import org.hawkular.datamining.api.ModelOptimization;
import org.hawkular.datamining.api.TimeSeriesModel;
import org.hawkular.datamining.api.model.DataPoint;
import org.hawkular.datamining.engine.EngineLogger;

import com.google.common.collect.EvictingQueue;

/**
 * @author Pavol Loffay
 */
public class DoubleExponentialSmoothing implements TimeSeriesModel {

    public static final double DEFAULT_LEVEL_SMOOTHING = 0.4;
    public static final double DEFAULT_TREND_SMOOTHING = 0.1;

    public static int MIN_BUFFER_SIZE = 5;

    private final double levelSmoothing;
    private final double trendSmoothing;

    private double level;
    private double slope;

    private AccuracyStatistics initAccuracy;
    private EvictingQueue<DataPoint> oldPoints;


    public DoubleExponentialSmoothing() {
        this(DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING);
    }

    /**
     * Double exponential smoothing - additive variant
     */
    public DoubleExponentialSmoothing(double levelSmoothing, double trendSmoothing) {
        if (levelSmoothing < 0.0 || levelSmoothing > 1.0) {
            throw new IllegalArgumentException("Level parameter should be in interval 0-1");
        }
        if (trendSmoothing < 0.0 || trendSmoothing > 1.0) {
            throw new IllegalArgumentException("Trend parameter should be in 0-1");
        }

        this.levelSmoothing = levelSmoothing;
        this.trendSmoothing = trendSmoothing;
        this.oldPoints = EvictingQueue.create(MIN_BUFFER_SIZE);
    }

    @Override
    public AccuracyStatistics init(List<DataPoint> dataPoints) {

        if (dataPoints == null || dataPoints.size() < MIN_BUFFER_SIZE) {
            throw new IllegalArgumentException("For init are required " + MIN_BUFFER_SIZE + " points.");
        }

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
    public void learn(DataPoint dataPoint) {
        learn(Arrays.asList(dataPoint));
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {
        for (DataPoint point: dataPoints) {

            oldPoints.add(point);

            if (oldPoints.remainingCapacity() == 1) {
                // compute level, trend
                initParams(oldPoints.iterator());
                oldPoints.forEach(oldPoint -> updateState(oldPoint));
                continue;
            }

            if (oldPoints.remainingCapacity() > 0) {
                continue;
            }

            updateState(point);
        }
    }

    @Override
    public DataPoint forecast() {
        double prediction = calculatePrediction(1);
        return new DataPoint(prediction, 0L); //TODO should be or 1?
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {

        List<DataPoint> result = new ArrayList<>(nAhead);
        for (int i = 0; i < nAhead; i++) {
            double prediction = calculatePrediction(i);
            DataPoint predictedPoint = new DataPoint(prediction,(long) i);

            result.add(predictedPoint);
        }

        return result;
    }

    @Override
    public AccuracyStatistics statistics() {
        return initAccuracy;
    }

    private void updateState(DataPoint point) {
        double level_old = level;
        level = levelSmoothing * point.getValue() + (1 - levelSmoothing) * (level + slope);
        slope = trendSmoothing * (level - level_old) + (1 - trendSmoothing) * (slope);
    }

    private double calculatePrediction(int nAhead) {
        return level + slope * nAhead;
    }

    private void initParams(Iterator<DataPoint> dataPointStream) {
        int count = 0;

        if (!dataPointStream.hasNext()) {
            slope = 0;
            level = 0;
        }

        DataPoint previous = dataPointStream.next();
        level = previous.getValue();
        while (dataPointStream.hasNext()) {
            DataPoint current = dataPointStream.next();

            slope += current.getValue() - previous.getValue();
            count++;
        }

        slope = slope / count;
    }

    public static class Optimizer implements ModelOptimization {

        @Override
        public TimeSeriesModel minimizedMSE(List<DataPoint> dataPoints) {

            MultivariateFunctionMappingAdapter constFunction = costFunction(dataPoints);

            int maxIter = 1000;
            int maxEval = 1000;

            // Nelder-Mead Simplex
            SimplexOptimizer nelderSimplexOptimizer = new SimplexOptimizer(0.0001, 0.0001);
            PointValuePair nelderResult = nelderSimplexOptimizer.optimize(
                    GoalType.MINIMIZE, new MaxIter(maxIter), new MaxEval(maxEval),
                    new InitialGuess(new double[]{DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING}),
                    new ObjectiveFunction(constFunction),
                    new NelderMeadSimplex(2));

            double[] param = constFunction.unboundedToBounded(nelderResult.getPoint());

            DoubleExponentialSmoothing bestModel = new DoubleExponentialSmoothing(param[0], param[1]);
            bestModel.init(dataPoints);

            return bestModel;
        }

        private MultivariateFunctionMappingAdapter costFunction(final List<DataPoint> dataPoints) {
            // func for minimization
            MultivariateFunction multivariateFunction = point -> {

                double alpha = point[0];
                double beta = point[1];

                DoubleExponentialSmoothing doubleExponentialSmoothing = new DoubleExponentialSmoothing(alpha, beta);
                AccuracyStatistics accuracyStatistics = doubleExponentialSmoothing.init(dataPoints);

                EngineLogger.LOGGER.tracef("%s MSE = %s, alpha=%f, beta=%f\n",
                        accuracyStatistics.getMse(), alpha, beta);
                return accuracyStatistics.getMse();
            };
            MultivariateFunctionMappingAdapter multivariateFunctionMappingAdapter =
                    new MultivariateFunctionMappingAdapter(multivariateFunction,
                            new double[]{0.0, 0.0}, new double[]{1, 1});

            return multivariateFunctionMappingAdapter;
        }
    }
}
