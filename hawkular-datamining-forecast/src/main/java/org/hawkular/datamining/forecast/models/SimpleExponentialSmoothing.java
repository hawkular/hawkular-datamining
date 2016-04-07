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
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.Logger;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;

/**
 * Simple exponential smoothing
 * Works well for stationary data when there is no trend in data. For trending data forecasts produce high MSE due to
 * flat forecast function.
 *
 * <p>
 * When smoothing parameters are smaller more weights are added to the observations from distant past - it makes it
 * model more robust.
 *
 * <p>
 * Equations:
 * <ul>
 *  <li> level<sub>t</sub> = alpha*y<sub>t</sub> + (1-alpha)level<sub>t-1</sub> </li>
 *  <li> forecast<sub>t+h</sub> = level<sub>t</sub> </li>
 * </ul>
 *
 * @author Pavol Loffay
 */
public class SimpleExponentialSmoothing extends AbstractExponentialSmoothing {

    public static final double DEFAULT_LEVEL_SMOOTHING = 0.4;
    public static final double MIN_LEVEL_SMOOTHING = 0.0001;
    public static final double MAX_LEVEL_SMOOTHING = 0.9999;

    private final double levelSmoothing;

    private State state;

    public static class State {

        protected double level;

        public State(double level) {
            this.level = level;
        }
    }


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
    public String name() {
        return "Simple exponential smoothing";
    }

    @Override
    public int numberOfParams() {
        return 2;
    }

    @Override
    public int minimumInitSize() {
        return 0;
    }

    @Override
    protected State initState(List<DataPoint> initData) {

        if (initData.isEmpty()) {
            throw new IllegalArgumentException("For init is required at least one point");
        }

        double level;

        if (initData.size() == 1) {
            level = initData.get(0).getValue();
        } else {
            // mean
            Double sum = initData.stream().map(dataPoint -> dataPoint.getValue())
                    .reduce((aDouble, aDouble2) -> aDouble + aDouble2).get();
            level = sum / (double) initData.size();
        }

        state = new State(level);
        return state;
    }

    @Override
    protected State state() {
        return state;
    }

    @Override
    protected void updateState(DataPoint dataPoint) {
        state.level = levelSmoothing*dataPoint.getValue() + (1 - levelSmoothing)*state.level;
    }

    @Override
    protected double calculatePrediction(int nAhead, DataPoint learnDataPoint) {
        return state.level;
    }

    public static Optimizer optimizer() {
        return new Optimizer();
    }

    @Override
    public String toString() {
        return "SimpleExponentialSmoothing{" +
                "alpha=" + levelSmoothing +
                ", level=" + state.level +
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
