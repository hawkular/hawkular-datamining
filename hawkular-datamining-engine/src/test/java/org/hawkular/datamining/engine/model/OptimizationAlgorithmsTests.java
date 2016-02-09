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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateFunctionMappingAdapter;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.MultiDirectionalSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.hawkular.datamining.api.model.DataPoint;
import org.hawkular.datamining.engine.AccuracyStatistics;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class OptimizationAlgorithmsTests {

    private static List<DataPoint> metricData;

    @BeforeClass
    public static void init() throws IOException {
        metricData = CSVTimeSeriesReader.getData("ar2.csv");
    }

    @Test
    public void testOptimization() throws IOException {

        int maxEval = 1000;
        int maxIter = 1000;

        MultivariateFunctionMappingAdapter optimizationFnNelder = optimizationFn("Nelder");
        MultivariateFunctionMappingAdapter optimizationFnMulti = optimizationFn("Multi");
        MultivariateFunctionMappingAdapter optimizationFnBobyQA = optimizationFn("BobyQA");

        // Nelder-Mead Simplex
        SimplexOptimizer nelderSimplexOptimizer = new SimplexOptimizer(0.0001, 0.0001);
        PointValuePair nelderResult = nelderSimplexOptimizer.optimize(
                GoalType.MINIMIZE, new MaxIter(maxIter), new MaxEval(maxEval),
                new InitialGuess(new double[]{0.4, 0.1}), new ObjectiveFunction(optimizationFnNelder),
                new NelderMeadSimplex(2));

        // Multi Directional Simplex
        SimplexOptimizer multiDirectSimplexOptimizer = new SimplexOptimizer(0.0001, 0.0001);
        PointValuePair multiResult = multiDirectSimplexOptimizer.optimize(
                GoalType.MINIMIZE, new MaxIter(maxIter), new MaxEval(maxEval),
                new InitialGuess(new double[]{0.4, 0.1}), new ObjectiveFunction(optimizationFnMulti),
                new MultiDirectionalSimplex(2));

        // BobyQA
        BOBYQAOptimizer bobyqaOptimizer = new BOBYQAOptimizer(6);
        PointValuePair bobyQAResult = bobyqaOptimizer.optimize(
                GoalType.MINIMIZE, new MaxEval(maxEval), new MaxIter(maxIter),
                new InitialGuess(new double[]{0.4, 0.1}), new ObjectiveFunction(optimizationFnBobyQA),
                SimpleBounds.unbounded(2));

        System.out.format("\nNelder (%d eval) -> key = %s, fce minimum= %.20f", nelderSimplexOptimizer.getEvaluations(),
                Arrays.toString(optimizationFnNelder.unboundedToBounded(nelderResult.getKey())),
                nelderResult.getValue());
        System.out.format("\nMulti (%d eval) -> key = %s, fce minimum= %.20f",
                multiDirectSimplexOptimizer.getEvaluations(),
                Arrays.toString(optimizationFnMulti.unboundedToBounded(multiResult.getKey())),
                multiResult.getValue());
        System.out.format("\nBobyQA (%d eval)-> key = %s, fce minimum= %.20f",  bobyqaOptimizer.getEvaluations(),
                Arrays.toString(optimizationFnBobyQA.unboundedToBounded(bobyQAResult.getKey())),
                bobyQAResult.getValue());
    }

    private MultivariateFunctionMappingAdapter optimizationFn(String algorithm) {

        final List<DataPoint> testData = metricData.subList(0, 50);
        // func for minimization
        MultivariateFunction multivariateFunction = point -> {

            double alpha = point[0];
            double beta = point[1];

            DoubleExponentialSmoothing doubleExponentialSmoothing = new DoubleExponentialSmoothing(alpha, beta);
            AccuracyStatistics accuracyStatistics = doubleExponentialSmoothing.init(testData);

            System.out.format("%s MSE = %s, alpha=%f, beta=%f\n", algorithm, accuracyStatistics.getMse(), alpha, beta);
            return accuracyStatistics.getMse();
        };
        MultivariateFunctionMappingAdapter multivariateFunctionMappingAdapter =
                new MultivariateFunctionMappingAdapter(multivariateFunction,
                        new double[]{0.0, 0.0}, new double[]{1, 1});

        return multivariateFunctionMappingAdapter;
    }
}
