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

import java.io.IOException;
import java.util.Arrays;

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
import org.hawkular.datamining.forecast.AbstractTest;
import org.hawkular.datamining.forecast.ModelData;
import org.hawkular.datamining.forecast.ModelReader;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Pavol Loffay
 */
public class OptimizationAlgorithmsTests extends AbstractTest {

    @Test
    public void testOptimizers() throws IOException {
        ModelData rModel = ModelReader.read("trendStatUpwardLowVar");

        int maxEval = 100000;
        int maxIter = 100000;

        MultivariateFunctionMappingAdapter optimizationFnNelder =
                DoubleExponentialSmoothing.optimizer().costFunction(rModel.getData());
        MultivariateFunctionMappingAdapter optimizationFnMulti =
                DoubleExponentialSmoothing.optimizer().costFunction(rModel.getData());
        MultivariateFunctionMappingAdapter optimizationFnBobyQA =
                DoubleExponentialSmoothing.optimizer().costFunction(rModel.getData());

        // Nelder-Mead Simplex
        System.out.format("\n\nNelder-Mead Simplex\n");
        SimplexOptimizer nelderSimplexOptimizer = new SimplexOptimizer(0.00001, 0.00001);
        PointValuePair nelderResult = nelderSimplexOptimizer.optimize(
                GoalType.MINIMIZE, new MaxIter(maxIter), new MaxEval(maxEval),
                new InitialGuess(new double[]{0.4, 0.1}), new ObjectiveFunction(optimizationFnNelder),
                new NelderMeadSimplex(2));

        // Multi Directional Simplex
        System.out.format("\n\nMulti Directional Simplex\n");
        SimplexOptimizer multiDirectSimplexOptimizer = new SimplexOptimizer(0.0001, 0.0001);
        PointValuePair multiResult = multiDirectSimplexOptimizer.optimize(
                GoalType.MINIMIZE, new MaxIter(maxIter), new MaxEval(maxEval),
                new InitialGuess(new double[]{0.2, 0.01}), new ObjectiveFunction(optimizationFnMulti),
                new MultiDirectionalSimplex(2));

        // BobyQA
        System.out.format("\n\nBobyQA\n");
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

        Assert.assertTrue(nelderSimplexOptimizer.getEvaluations() < multiDirectSimplexOptimizer.getEvaluations() &&
                nelderSimplexOptimizer.getEvaluations() < bobyqaOptimizer.getEvaluations());
    }
}
