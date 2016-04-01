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

/**
 * Triple exponential smoothing model also known as Holt-Winters model. This model implements additive variant.
 *
 * @author Pavol Loffay
 */
public class TripleExponentialSmoothing extends AbstractExponentialSmoothing {

    public static final double DEFAULT_LEVEL_SMOOTHING = 0.4;
    public static final double DEFAULT_TREND_SMOOTHING = 0.1;
    public static final double DEFAULT_SEASONAL_SMOOTHING = 0.1;

    public static final double MIN_LEVEL_SMOOTHING = 0.0001;
    public static final double MIN_TREND_SMOOTHING = 0.0001;
    public static final double MIN_SEASONAL_SMOOTHING = 0.0001;
    public static final double MAX_LEVEL_SMOOTHING = 0.9999;
    public static final double MAX_TREND_SMOOTHING = 0.9999;
    public static final double MAX_SEASONAL_SMOOTHING = 0.9999;

    private State state;
    private final double levelSmoothing;
    private final double trendSmoothing;
    private final double seasonalSmoothing;

    private final int periods;
    private int currentPeriod;


    public static class State extends DoubleExponentialSmoothing.State {
        protected double[] periods;

        public State(double level, double slope, double[] periods) {
            super(level, slope);
            this.periods = Arrays.copyOf(periods, periods.length);
        }
    }

    public TripleExponentialSmoothing(int periods) {
        this(periods, DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING, DEFAULT_SEASONAL_SMOOTHING);
    }

    public TripleExponentialSmoothing(double levelSmoothing, double trendSmoothing,
                                      double seasonalSmoothing, State state) {
        this(state.periods.length, levelSmoothing, trendSmoothing, seasonalSmoothing);
        this.state = state;
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
    public int minimumInitSize() {
        return periods*2;
    }

    public static State initState(List<DataPoint> dataPoints, int periods) {
        return new TripleExponentialSmoothing(periods).initState(dataPoints);
    }

    @Override
    protected State initState(List<DataPoint> dataPoints) {
        if (dataPoints.size()/periods < 2) {
            throw new IllegalArgumentException("At least two complete seasons are required");
        }

        AdditiveSeasonalDecomposition decomposition = new AdditiveSeasonalDecomposition(dataPoints, periods);
        double[] periodIndices = decomposition.decompose();

        // do regression on seasonally adjusted data points
        List<DataPoint> seasonal = decomposition.seasonal();
        SimpleRegression regression = new SimpleRegression();
        for (int i = 0; i < dataPoints.size(); i++) {
            regression.addData(i, dataPoints.get(i).getValue() - seasonal.get(i).getValue());
        }
        double level = regression.predict(0);
        double slope = regression.getSlope();

        int firstPeriod = periodIndex(dataPoints.get(0).getTimestamp());
        double[] switchedPeriods = rotatePeriods(periodIndices, firstPeriod);

        state = new State(level, slope, switchedPeriods);
        return state;
    }

    private double[] rotatePeriods(double[] periods, int firstPeriod) {
        double[] result = new double[periods.length];

        for (int i = 0; i < periods.length; i++) {
            result[i] = periods[(i + firstPeriod)%periods.length];
        }

        return result;
    }

    @Override
    protected TripleExponentialSmoothing.State state() {
        return state;
    }

    @Override
    protected void updateState(DataPoint point) {
        double oldLevel = state.level;
        double oldSlope = state.slope;
        int periodToCount = periodIndex(point.getTimestamp());

        state.level = levelSmoothing*(point.getValue() - state.periods[periodToCount]) +
                (1 - levelSmoothing)*(state.level + state.slope);
        state.slope = trendSmoothing*(state.level - oldLevel) + (1 - trendSmoothing)*state.slope;
        state.periods[periodToCount] = seasonalSmoothing*(point.getValue() - oldLevel - oldSlope) +
                (1 - seasonalSmoothing)*state.periods[periodToCount];

        currentPeriod = periodIndex(point.getTimestamp());
    }

    @Override
    protected double calculatePrediction(int nAhead, DataPoint learnDataPoint) {

        int previousPeriodIndex = currentPeriod;
        if (learnDataPoint != null) {
            previousPeriodIndex = periodIndex(learnDataPoint.getTimestamp() - 1);
            previousPeriodIndex += previousPeriodIndex < 0 ? periods : 0;
        }

        Double forecastPeriod = state.periods[((previousPeriodIndex + nAhead) % periods)];

        return state.level + nAhead*state.slope + forecastPeriod;
    }

    private int periodIndex(long timestamp) {
        return (int) (timestamp % periods);
    }

    @Override
    public String toString() {
        return "TripleExponentialSmoothing{" +
                "levelSmoothing=" + levelSmoothing +
                ", trendSmoothing=" + trendSmoothing +
                ", seasonalSmoothing=" + seasonalSmoothing +
                ", level=" + state.level +
                ", slope=" + state.slope +
                ", periods=" + state.periods.length +
                ", periodsIndices=" + Arrays.toString(state.periods) +
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
            initState = TripleExponentialSmoothing.initState(dataPoints, periods);

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
