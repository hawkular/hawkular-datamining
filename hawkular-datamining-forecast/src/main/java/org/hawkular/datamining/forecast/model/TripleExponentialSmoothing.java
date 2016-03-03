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
import org.hawkular.datamining.forecast.AccuracyStatistics;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.Logger;

import com.google.common.collect.EvictingQueue;

/**
 * @author Pavol Loffay
 */
public class TripleExponentialSmoothing implements TimeSeriesModel {

    public static final double DEFAULT_LEVEL_SMOOTHING = 0.4;
    public static final double DEFAULT_TREND_SMOOTHING = 0.1;
    public static final double DEFAULT_SEASONAL_SMOOTHING = 0.1;

    private final double levelSmoothing;
    private final double trendSmoothing;
    private final double seasonalSmoothing;

    private double level;
    private double slope;
    private double[] periods;
    private int currentPeriod;

    private long counter;
    private double sse;
    private double absSum;
    private AccuracyStatistics initAccuracy;
    private AccuracyStatistics runAccuracy;
    private EvictingQueue<DataPoint> window;

    public TripleExponentialSmoothing(int periods) {
        this(periods, DEFAULT_LEVEL_SMOOTHING, DEFAULT_TREND_SMOOTHING, DEFAULT_SEASONAL_SMOOTHING);
    }

    public TripleExponentialSmoothing(int periods, double levelSmoothing, double trendSmoothing,
                                      double seasonalSmoothing) {

        if (levelSmoothing < 0.0 || levelSmoothing > 1.0) {
            throw new IllegalArgumentException("Level parameter should be in interval 0-1");
        }
        if (trendSmoothing < 0.0 || trendSmoothing > 1.0) {
            throw new IllegalArgumentException("Trend parameter should be in 0-1");
        }
        if (seasonalSmoothing < 0.0 || seasonalSmoothing > 1.0) {
            throw new IllegalArgumentException("Trend parameter should be in 0-1");
        }

        this.periods = new double[periods];
        this.levelSmoothing = levelSmoothing;
        this.trendSmoothing = trendSmoothing;
        this.seasonalSmoothing = seasonalSmoothing;
        this.window = EvictingQueue.create(periods * 2);
    }

    @Override
    public AccuracyStatistics init(List<DataPoint> dataPoints) {

        initState(dataPoints);
        learn(dataPoints);
        initAccuracy = new AccuracyStatistics(sse ,sse/(double)dataPoints.size(), absSum/(double)dataPoints.size());
        sse = 0d;
        absSum = 0d;
        counter = 0L;

        return initAccuracy;
    }

    @Override
    public void learn(DataPoint dataPoint) {

        additiveUpdate(dataPoint);
        currentPeriod = periodIndex(dataPoint.getTimestamp());
    }

    private void additiveUpdate(DataPoint point) {
        double error = point.getValue() - forecast().getValue();
        sse += error * error;
        absSum += Math.abs(error);
        counter++;

        int currentPeriod = periodIndex(point.getTimestamp());
        double oldLevel = level;
        double oldSlope = slope;
        level = levelSmoothing*(point.getValue() - periods[currentPeriod]) + (1 - levelSmoothing)*(level +  slope);
        slope = trendSmoothing*(level - oldLevel) + (1-trendSmoothing)*slope;
        periods[currentPeriod] = seasonalSmoothing*(point.getValue() - oldLevel - oldSlope) +
                (1 - seasonalSmoothing)*periods[currentPeriod];
    }

    private void multiplicativeUpdate(DataPoint point) {

        double error = point.getValue() - forecast().getValue();
        sse += error * error;
        absSum += Math.abs(error);
        counter++;

        int currentPeriod = periodIndex(point.getTimestamp());
        double oldLevel = level;
        double oldSlope = slope;

        // multiplicative
        level = (levelSmoothing*point.getValue())/ periods[currentPeriod] + (1 - levelSmoothing)*(level + slope);
        slope = trendSmoothing*(level - oldLevel) + (1 - trendSmoothing)*slope;
        periods[currentPeriod] = (seasonalSmoothing*point.getValue())/(oldLevel + oldSlope) +
                (1 - seasonalSmoothing)*periods[currentPeriod];

    }

    @Override
    public void learn(List<DataPoint> dataPoints) {
        dataPoints.forEach(dataPoint -> {
            learn(dataPoint);
        });
    }

    private int periodIndex(long timestamp) {
        return (int) (timestamp % periods.length);
    }

    private long forecastIndex(long nAhead) {
        return (nAhead - 1) % periods.length + 1;
    }

    private void initState(List<DataPoint> dataPoints) {
        // initialize trend, level, season

        level = 0;
        slope = 0;
        periods = new double[periods.length];

        int completeSeasons = dataPoints.size() / periods.length;
        if (completeSeasons < 2) {
            throw new IllegalArgumentException("At least two complete seasons are required");
        }

        // level - average of the first season
        for (int period = 0; period < periods.length; period++) {
            level += dataPoints.get(period).getValue();
        }
        level /= periods.length;

        // slope
        for (int period = 0; period < periods.length; period++) {
            slope += (dataPoints.get(periods.length + period).getValue() - dataPoints.get(period).getValue()) /
                    (double) periods.length;
        }
        slope /= periods.length;

        // level and trend - linear regression
//        SimpleRegression regression = new SimpleRegression();
//        dataPoints.forEach(dataPoint -> regression.addData(dataPoint.getTimestamp(), dataPoint.getValue()));
//        level = regression.predict(dataPoints.get(0).getTimestamp());
//        slope = regression.getSlope();

        // periods
        for (int period = 0; period < periods.length; period++) {
            periods[period] = dataPoints.get(period).getValue() - level;
        }

//         seasons averages
//        http://www.itl.nist.gov/div898/handbook/pmc/section4/pmc435.htm
//        sine long failed to pass
//        double[] seasonsAverages = new double[completeSeasons];
//        for (int season = 0; season < completeSeasons; season++) {
//            for (int period = 0; period < periods.length; period++) {
//                seasonsAverages[season] += dataPoints.get(season + period).getValue();
//            }
//            seasonsAverages[season] /= (double)periods.length;
//        }
        // sum of averages through seasons
//        for (int period = 0; period < periods.length; period++) {
//            for (int season = 0; season < completeSeasons; season++) {
//                periods[period] += dataPoints.get(period + (periods.length*season)).getValue() /
//                        seasonsAverages[season];
//            }
//            periods[period] /= completeSeasons;
//        }
    }

    @Override
    public DataPoint forecast() {
        double forecast = level + 1*slope + periods[(currentPeriod + 1) % periods.length];
        return new DataPoint(forecast, 0L);
    }

    @Override
    public List<DataPoint> forecast(int nAhead) {


        List<DataPoint> result = new ArrayList<>(nAhead);

        for (long i = 0; i < nAhead; i++) {
            double forecast = (level + i * slope) * forecastIndex(i);
            result.add(new DataPoint(forecast, i));
        }

        throw new IllegalArgumentException();
//        return result;
    }

    @Override
    public AccuracyStatistics initStatistics() {
        return initAccuracy;
    }

    @Override
    public AccuracyStatistics runStatistics() {
        return runAccuracy;
    }

    @Override
    public String name() {
        return "Triple exponential smoothing";
    }

    @Override
    public int numberOfParams() {
        return 6;
    }

    public static Optimizer optimizer() {
        return new Optimizer();
    }

    @Override
    public String toString() {
        return "TripleExponentialSmoothing{" +
                "levelSmoothing=" + levelSmoothing +
                ", trendSmoothing=" + trendSmoothing +
                ", seasonalSmoothing=" + seasonalSmoothing +
                ", level=" + level +
                ", slope=" + slope +
                ", periods=" + Arrays.toString(periods) +
                ", currentPeriod=" + currentPeriod +
                '}';
    }

    public static class Optimizer implements ModelOptimizer {

        private double[] result;

        @Override
        public double[] result() {
            return result;
        }

        private int seasons;
        public Optimizer setSeasons(int seasons) {
            this.seasons = seasons;
            return this;
        }

        @Override
        public TimeSeriesModel minimizedMSE(List<DataPoint> dataPoints) {

            MultivariateFunctionMappingAdapter constFunction = costFunction(dataPoints);

            int maxIter = 1000;
            int maxEval = 1000;

            // Nelder-Mead Simplex
            SimplexOptimizer nelderSimplexOptimizer = new SimplexOptimizer(0.0001, 0.0001);
            PointValuePair nelderResult = nelderSimplexOptimizer.optimize(
                    GoalType.MINIMIZE, new MaxIter(maxIter), new MaxEval(maxEval),
                    new InitialGuess(new double[]{DEFAULT_LEVEL_SMOOTHING,
                            DEFAULT_TREND_SMOOTHING, DEFAULT_SEASONAL_SMOOTHING}),
                    new ObjectiveFunction(constFunction),
                    new NelderMeadSimplex(3));

            result = constFunction.unboundedToBounded(nelderResult.getPoint());
            Logger.LOGGER.debugf("Optimizer best alpha: %.5f, beta %.5f, gamma %.5f", this.result[0], this.result[1],
                    this.result[2]);

            TripleExponentialSmoothing bestModel = new TripleExponentialSmoothing(seasons,
                    result[0], result[1], result[2]);
            bestModel.init(dataPoints);

            return bestModel;
        }

        private MultivariateFunctionMappingAdapter costFunction(final List<DataPoint> dataPoints) {
            // func for minimization
            MultivariateFunction multivariateFunction = point -> {

                double alpha = point[0];
                double beta = point[1];
                double gamma = point[2];

                TripleExponentialSmoothing tripleExponentialSmoothing = new TripleExponentialSmoothing(seasons,
                        alpha, beta, gamma);
                AccuracyStatistics accuracyStatistics = tripleExponentialSmoothing.init(dataPoints);

                Logger.LOGGER.tracef("MSE = %s, alpha=%f, beta=%f, gamma=%f",  accuracyStatistics.getMse(),
                        alpha, beta, gamma);
                return accuracyStatistics.getMse();
            };
            MultivariateFunctionMappingAdapter multivariateFunctionMappingAdapter =
                    new MultivariateFunctionMappingAdapter(multivariateFunction,
                            new double[]{0.0001, 0.0001, 0.0001}, new double[]{0.9999, 0.9999, 0.9999});

            return multivariateFunctionMappingAdapter;
        }
    }
}
