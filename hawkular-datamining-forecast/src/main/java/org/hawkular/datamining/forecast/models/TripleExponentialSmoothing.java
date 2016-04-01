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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.MathIllegalStateException;
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
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.Logger;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;
import org.hawkular.datamining.forecast.utils.AdditiveSeasonalDecomposition;
import org.hawkular.datamining.forecast.utils.AutomaticPeriodIdentification;

import com.google.common.collect.EvictingQueue;

/**
 * Triple exponential smoothing model also known as Holt-Winters model. This model implements additive variant.
 *
 * @author Pavol Loffay
 */
public class TripleExponentialSmoothing implements TimeSeriesModel {

    public static final double DEFAULT_LEVEL_SMOOTHING = 0.4;
    public static final double DEFAULT_TREND_SMOOTHING = 0.1;
    public static final double DEFAULT_SEASONAL_SMOOTHING = 0.1;

    public static final double MIN_LEVEL_SMOOTHING = 0.0001;
    public static final double MIN_TREND_SMOOTHING = 0.0001;
    public static final double MIN_SEASONAL_SMOOTHING = 0.0001;
    public static final double MAX_LEVEL_SMOOTHING = 0.9999;
    public static final double MAX_TREND_SMOOTHING = 0.9999;
    public static final double MAX_SEASONAL_SMOOTHING = 0.9999;

    private final double levelSmoothing;
    private final double trendSmoothing;
    private final double seasonalSmoothing;

    private final int periods;
    private int currentPeriod;
    private State initState;

    private long counter;
    private double sse;
    private double absSum;
    private AccuracyStatistics initAccuracy;
    private final EvictingQueue<DataPoint> window;


    public static class State {
        public State(double level, double slope, double[] periods) {
            this.level = level;
            this.slope = slope;
            this.periods = Arrays.copyOf(periods, periods.length);
        }

        private double level;
        private double slope;
        private double[] periods;
    }

    public TripleExponentialSmoothing(int periods) {
        this(periods, DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING, DEFAULT_SEASONAL_SMOOTHING);
    }

    public TripleExponentialSmoothing(double levelSmoothing, double trendSmoothing,
                                      double seasonalSmoothing, State initState) {
        this(initState.periods.length, levelSmoothing, trendSmoothing, seasonalSmoothing);

        this.initState = initState;
    }

    public TripleExponentialSmoothing(int periods, double levelSmoothing, double trendSmoothing,
                                      double seasonalSmoothing) {

        if (levelSmoothing < MIN_LEVEL_SMOOTHING || levelSmoothing > MAX_LEVEL_SMOOTHING) {
            throw new IllegalArgumentException("Level smoothing should be in interval 0-1");
        }
        if (trendSmoothing < MIN_TREND_SMOOTHING || trendSmoothing > MAX_TREND_SMOOTHING) {
            throw new IllegalArgumentException("Trend smoothing should be in 0-1");
        }
        if (seasonalSmoothing < MIN_SEASONAL_SMOOTHING || seasonalSmoothing > MAX_SEASONAL_SMOOTHING) {
            throw new IllegalArgumentException("Seasonal smoothing should be in 0-1");
        }

        if (periods < 2) {
            throw new IllegalArgumentException("Periods < 2, use non seasonal model.");
        }

        this.periods = periods;

        this.levelSmoothing = levelSmoothing;
        this.trendSmoothing = trendSmoothing;
        this.seasonalSmoothing = seasonalSmoothing;
        this.window = EvictingQueue.create(periods * 2);
    }

    @Override
    public AccuracyStatistics init(List<DataPoint> dataPoints) {

        if (initState == null) {
            initState(dataPoints);
        }

        learn(dataPoints);
        initAccuracy = new AccuracyStatistics(sse, sse/(double)dataPoints.size(), absSum/(double)dataPoints.size());
        sse = 0d;
        absSum = 0d;
        counter = 0L;

        return initAccuracy;
    }

    @Override
    public void learn(DataPoint dataPoint) {

        window.add(dataPoint);

        additiveUpdate(dataPoint);

        currentPeriod = periodIndex(dataPoint.getTimestamp());
    }

    @Override
    public void learn(List<DataPoint> dataPoints) {

        if (initState == null && window.remainingCapacity() - dataPoints.size() < 1) {
            List<DataPoint> initData = new ArrayList<>(window);
            initData.addAll(dataPoints);
            initState(initData);
        }

        dataPoints.forEach(dataPoint -> {
            learn(dataPoint);
        });
    }

    private int periodIndex(long timestamp) {
        return (int) (timestamp % periods);
    }

    private void additiveUpdate(DataPoint point) {
        double error = point.getValue() - forecast().getValue();
        sse += error*error;
        absSum += Math.abs(error);
        counter++;

        double oldLevel = initState.level;
        double oldSlope = initState.slope;
        int periodToCount = currentPeriod = periodIndex(point.getTimestamp());

        initState.level = levelSmoothing*(point.getValue() - initState.periods[periodToCount]) +
                (1 - levelSmoothing)*(initState.level + initState.slope);
        initState.slope = trendSmoothing*(initState.level - oldLevel) + (1 - trendSmoothing)*initState.slope;
        initState.periods[periodToCount] = seasonalSmoothing*(point.getValue() - oldLevel - oldSlope) +
                (1 - seasonalSmoothing)*initState.periods[periodToCount];
    }

    private State initState(List<DataPoint> dataPoints) {
        if (dataPoints.size()/periods < 2) {
            throw new IllegalArgumentException("At least two complete seasons are required");
        }

        AdditiveSeasonalDecomposition decomposition = new AdditiveSeasonalDecomposition(dataPoints, periods);
        double[] periods = decomposition.decompose();

        // do regression on seasonally adjusted data points
        List<DataPoint> seasonal = decomposition.seasonal();
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < dataPoints.size(); i++) {
            regression.addData(i, dataPoints.get(i).getValue() - seasonal.get(i).getValue());
        }
        double level = regression.predict(0);
        double slope = regression.getSlope();

        int firstPeriod = periodIndex(dataPoints.get(0).getTimestamp());
        double[] rotatedPeriods = rotatePeriods(periods, firstPeriod);

        initState = new State(level, slope, rotatedPeriods);
        return initState;
    }

    @Override
    public DataPoint forecast() {
        double forecast = initState.level + initState.slope + forecastSeason(1);
        return new DataPoint(forecast, 1L);
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {
        List<DataPoint> result = new ArrayList<>(nAhead);

        for (long i = 1; i <= nAhead; i++) {
            double forecast = initState.level + i*initState.slope + forecastSeason(i);
            result.add(new DataPoint(forecast, i));
        }

        return result;
    }

    private double forecastSeason(long nAhead) {
        return  initState.periods[((int) ((currentPeriod + nAhead) % periods))];
    }

    private double[] rotatePeriods(double[] periods, int firstPeriod) {
        double[] result = new double[periods.length];

        for (int i = 0; i < periods.length; i++) {
            result[i] = periods[(i + firstPeriod)%periods.length];
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
        return "Triple exponential smoothing";
    }

    @Override
    public int numberOfParams() {
        return 5 + periods;
    }

    @Override
    public String toString() {
        return "TripleExponentialSmoothing{" +
                "levelSmoothing=" + levelSmoothing +
                ", trendSmoothing=" + trendSmoothing +
                ", seasonalSmoothing=" + seasonalSmoothing +
                ", level=" + initState.level +
                ", slope=" + initState.slope +
                ", periods=" + initState.periods.length +
                ", periodsIndices=" + Arrays.toString(initState.periods) +
                ", currentPeriod=" + currentPeriod +
                '}';
    }

    public static Optimizer optimizer(int periods) {
        return new Optimizer(periods);
    }

    public static Optimizer optimizer() {
        return new Optimizer();
    }

    public static class Optimizer implements ModelOptimizer {
        private static final int MAX_ITER = 10000;
        private static final int MAX_EVAL = 10000;

        private final Integer definedPeriods;

        private Integer periods;
        private State initState;
        private double[] result;


        public Optimizer() {
            this(null);
        }

        public Optimizer(Integer periods) {
            this.definedPeriods = periods;
        }

        public Integer getPeriods() {
            return definedPeriods == null ? periods : definedPeriods;
        }

        @Override
        public double[] result() {
            return result;
        }

        @Override
        public TimeSeriesModel minimizedMSE(List<DataPoint> dataPoints) {
            periods = definedPeriods == null ? AutomaticPeriodIdentification.periods(dataPoints) : definedPeriods;
            initState = new TripleExponentialSmoothing(periods).initState(dataPoints);

            int periodsToOptimize = periods;

            // Nelder-Mead Simplex
            try {
                double[] initialGuess = initialGuess(initState, periodsToOptimize);
                MultivariateFunctionMappingAdapter costFunction = costFunction(dataPoints, periodsToOptimize);
                result = optimize(initialGuess, costFunction);
            } catch (MathIllegalStateException ex) {
                // optimize without seasons
                Logger.LOGGER.errorf("Triple exponential smoothing optimizer failed to optimize periods");
                periodsToOptimize = 0;
                double[] initialGuess = initialGuess(initState, periodsToOptimize);
                MultivariateFunctionMappingAdapter costFunction = costFunction(dataPoints, periodsToOptimize);
                result = optimize(initialGuess, costFunction);
            }

            Logger.LOGGER.debugf("Optimizer best alpha: %.5f, beta %.5f, gamma %.5f", this.result[0], this.result[1],
                    this.result[2]);

            TripleExponentialSmoothing bestModel = model(result, periodsToOptimize);
            bestModel.init(dataPoints);

            return bestModel;
        }

        private MultivariateFunctionMappingAdapter costFunction(final List<DataPoint> dataPoints,
                                                                final int periodsToOptimize) {
            // func for minimization
            MultivariateFunction multivariateFunction = point -> {

                if (point[1] >= point[0]) {
                    return Double.POSITIVE_INFINITY;
                }

                TripleExponentialSmoothing tripleExponentialSmoothing = model(point, periodsToOptimize);
                AccuracyStatistics accuracyStatistics = tripleExponentialSmoothing.init(dataPoints);

                Logger.LOGGER.tracef("MSE = %s, alpha=%f, beta=%f, gamma=%f",  accuracyStatistics.getMse(),
                        point[0], point[1], point[2]);
                return accuracyStatistics.getMse();
            };

            double[][] minMax = parametersMinMax(periodsToOptimize);
            MultivariateFunctionMappingAdapter multivariateFunctionMappingAdapter =
                    new MultivariateFunctionMappingAdapter(multivariateFunction, minMax[0], minMax[1]);

            return multivariateFunctionMappingAdapter;
        }

        private double[] initialGuess(State state, int periodsToOptimize) {
            double[] initialGuess = new double[5 + periodsToOptimize];
            initialGuess[0] = DEFAULT_LEVEL_SMOOTHING;
            initialGuess[1] = DEFAULT_TREND_SMOOTHING;
            initialGuess[2] = DEFAULT_SEASONAL_SMOOTHING;
            initialGuess[3] = state.level;
            initialGuess[4] = state.slope;

            for (int i = 5; i < 5 + periodsToOptimize; i++) {
                initialGuess[i] = state.periods[i - 5];
            }

            return initialGuess;
        }

        private double[][] parametersMinMax(int periodsToOptimize) {
            double[] min = new double[5 + periodsToOptimize];
            double[] max = new double[5 + periodsToOptimize];
            min[0] = 0.0001;
            min[1] = 0.0001;
            min[2] = 0.0001;
            min[3] = Double.NEGATIVE_INFINITY;
            min[4] = Double.NEGATIVE_INFINITY;

            max[0] = 0.9999;
            max[1] = 0.9999;
            max[2] = 0.9999;
            max[3] = Double.POSITIVE_INFINITY;
            max[4] = Double.POSITIVE_INFINITY;

            for (int i = 5; i < 5 + periodsToOptimize; i++) {
                min[i] = Double.NEGATIVE_INFINITY;
                max[i] = Double.POSITIVE_INFINITY;
            }

            return new double[][]{min, max};
        }

        private TripleExponentialSmoothing model(double[] point, int periodsToOptimize) {

            double alpha = point[0];
            double beta = point[1];
            double gamma = point[2];

            double level = point[3];
            double slope = point[4];

            double[] periods = new double[this.periods];
            for (int i = 0; i < this.periods; i++) {
                if (i < periodsToOptimize) {
                    periods[i] = point[5 + i];
                } else {
                    periods[i] = initState.periods[i];
                }
            }

            State state = new State(level, slope, periods);

            TripleExponentialSmoothing model = new TripleExponentialSmoothing(alpha, beta, gamma, state);
            return model;
        }

        private double[] optimize(double[] initialGuess, MultivariateFunctionMappingAdapter costFunction) {

            SimplexOptimizer optimizer = new SimplexOptimizer(0.0001, 0.0001);
            PointValuePair unBoundedResult = optimizer.optimize(
                    GoalType.MINIMIZE, new MaxIter(MAX_ITER), new MaxEval(MAX_EVAL),
                    new InitialGuess(initialGuess),
                    new ObjectiveFunction(costFunction),
                    new NelderMeadSimplex(initialGuess.length));

            return costFunction.unboundedToBounded(unBoundedResult.getPoint());
        }
    }
}
