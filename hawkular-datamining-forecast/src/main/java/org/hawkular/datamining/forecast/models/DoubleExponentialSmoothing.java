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
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.Logger;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;

/**
 * Double exponential smoothing (Holt's linear trend model)
 * Works well when data exhibits increasing or decreasing trend pattern.
 *
 * <p>
 * When smoothing parameters are smaller more weights are added to the observations from distant past - it makes it
 * model more robust.
 *
 * <p>
 * Equations:
 * <ul>
 *  <li> level<sub>t</sub> = alpha*y<sub>t</sub> + (1-alpha)*(level<sub>t-1</sub>+trend<sub>t-1</sub>) </li>
 *  <li> trend<sub>t</sub> = beta*(level<sub>t</sub>-level<sub>t-1</sub>) + (1-beta)*trend<sub>t-1</sub> </li>
 *  <li> forecast<sub>t+h</sub> = level<sub>t</sub> + h*trend<sub>t</sub> </li>
 * </ul>
 *
 * @author Pavol Loffay
 */
public class DoubleExponentialSmoothing extends AbstractExponentialSmoothing {

    public static final double DEFAULT_LEVEL_SMOOTHING = 0.4;
    public static final double DEFAULT_TREND_SMOOTHING = 0.1;
    public static final double MIN_LEVEL_TREND_SMOOTHING = 0.0001;
    public static final double MAX_LEVEL_TREND_SMOOTHING = 0.9999;

    private State state;
    private final double levelSmoothing;
    private final double trendSmoothing;


    public static class State extends SimpleExponentialSmoothing.State {
        protected double slope;

        public State(double level, double slope) {
            super(level);
            this.slope = slope;
        }
    }

    public DoubleExponentialSmoothing() {
        this(DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING);
    }

    public DoubleExponentialSmoothing(double levelSmoothing, double trendSmoothing) {
        if (levelSmoothing < MIN_LEVEL_TREND_SMOOTHING || levelSmoothing > MAX_LEVEL_TREND_SMOOTHING) {
            throw new IllegalArgumentException("Level parameter should be in interval 0-1");
        }
        if (trendSmoothing < MIN_LEVEL_TREND_SMOOTHING || trendSmoothing > MAX_LEVEL_TREND_SMOOTHING) {
            throw new IllegalArgumentException("Trend parameter should be in 0-1");
        }

        this.levelSmoothing = levelSmoothing;
        this.trendSmoothing = trendSmoothing;
    }

    @Override
    public String name() {
        return "Double exponential smoothing";
    }

    @Override
    public int numberOfParams() {
        return 4;
    }

    @Override
    public int minimumInitSize() {
        return 2;
    }

    @Override
    protected State initState(List<DataPoint> initData) {

        if (initData.size() < minimumInitSize()) {
            throw new IllegalArgumentException("For init are required " + minimumInitSize() + " points.");
        }

        double level;
        double slope;

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

        state = new State(level, slope);
        return state;
    }

    @Override
    protected SimpleExponentialSmoothing.State state() {
        return state;
    }

    @Override
    protected void updateState(DataPoint dataPoint) {
        double oldLevel = state.level;
        state.level = levelSmoothing*dataPoint.getValue() + (1 - levelSmoothing)*(state.level + state.slope);
        state.slope = trendSmoothing*(state.level - oldLevel) + (1 - trendSmoothing)*(state.slope);
    }

    @Override
    protected double calculatePrediction(int nAhead) {
        return state.level + state.slope * nAhead;
    }

    public static Optimizer optimizer() {
        return new Optimizer();
    }

    @Override
    public String toString() {
        return "DoubleExponentialSmoothing{" +
                "alpha=" + levelSmoothing +
                ", beta=" + trendSmoothing +
                ", level=" + state.level +
                ", slope=" + state.slope +
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
