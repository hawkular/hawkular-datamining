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

package org.hawkular.datamining.forecast.models.performance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.hawkular.datamining.forecast.AbstractTest;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.ImmutableMetricContext;
import org.hawkular.datamining.forecast.ModelData;
import org.hawkular.datamining.forecast.ModelReader;
import org.hawkular.datamining.forecast.models.DoubleExponentialSmoothing;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class OptimizationAlgorithmsTests extends AbstractTest {

    private int MAX_EVAL = 100000;
    private int MAX_ITER = 100000;

    private double[] INITIAL_GUESS = new double[] {
            DoubleExponentialSmoothing.DEFAULT_LEVEL_SMOOTHING, DoubleExponentialSmoothing.DEFAULT_TREND_SMOOTHING};

    ModelData rModel; {
        try {
//            rModel = ModelReader.read("sineLowVarLong");
//            rModel = ModelReader.read("wnLowVariance");
//            rModel = ModelReader.read("austourists");

            ModelData wn = ModelReader.read("wnLowVariance");
            ModelData trend = ModelReader.read("trendStatUpwardHighVar");
            ModelData sine = ModelReader.read("sineLowVarLong");
            List<DataPoint> combinedTs = new ArrayList<>();
            addData(combinedTs, wn.getData());
            addData(combinedTs, trend.getData());
            addData(combinedTs, sine.getData());
            sine.setData(combinedTs);

            rModel = sine;
        } catch (IOException e) {
        }
    }

    private MultivariateFunctionMappingAdapter objectiveFunction =
            DoubleExponentialSmoothing.optimizer().costFunction(rModel.getData());

    @Test
    public void performanceTest() throws IOException {
        final int numberOfExecutions = 5;

        testCase(numberOfExecutions, rModel, this::executeSimplex, "Simplex");
        testCase(numberOfExecutions, rModel, this::executeMultidirectionalSimplex, "Multidirectional Simplex");
        testCase(numberOfExecutions, rModel, this::executePowell, "Powell");
        testCase(numberOfExecutions, rModel, this::executeBobyQA, "BobyQA");
    }

    private void testCase(int numberOfExecutions, ModelData modelData, Function<ModelData, Long> fce, String name) {

        long sum = 0;
        for (int i = 0; i < numberOfExecutions; i++) {
            sum += fce.apply(modelData);
        }

        System.out.println(name + ", number of observations = " + modelData.getData().size());
        System.out.println("Number of executions = " + numberOfExecutions +
                ", average execution time = " + (sum/numberOfExecutions)/1e9);
    }

    private long executeSimplex(ModelData modelData) {
        long start = System.nanoTime();
        SimplexOptimizer optimizer = new SimplexOptimizer(0.00001, 0.00001);
        PointValuePair unbounded = optimizer.optimize(
                GoalType.MINIMIZE, new MaxIter(MAX_ITER), new MaxEval(MAX_EVAL),
                new InitialGuess(INITIAL_GUESS),
                new ObjectiveFunction(objectiveFunction),
                new NelderMeadSimplex(2));
        long executionTime = System.nanoTime() - start;

        printOptimizationResult(objectiveFunction, unbounded.getPoint(), modelData);

        return executionTime;
    }

    private long executeMultidirectionalSimplex(ModelData modelData) {
        long start = System.nanoTime();
        SimplexOptimizer optimizer = new SimplexOptimizer(0.00001, 0.00001);
        PointValuePair unbounded = optimizer.optimize(
                GoalType.MINIMIZE, new MaxIter(MAX_ITER), new MaxEval(MAX_EVAL),
                new InitialGuess(INITIAL_GUESS),
                new ObjectiveFunction(objectiveFunction),
                new MultiDirectionalSimplex(2));
        long executionTime = System.nanoTime() - start;

        printOptimizationResult(objectiveFunction, unbounded.getPoint(), modelData);

        return executionTime;
    }

    private long executeBobyQA(ModelData modelData) {
        long start = System.nanoTime();
        BOBYQAOptimizer optimizer = new BOBYQAOptimizer(6);
        PointValuePair bobyQAResult = optimizer.optimize(
                GoalType.MINIMIZE, new MaxEval(MAX_EVAL), new MaxIter(MAX_ITER),
                new InitialGuess(INITIAL_GUESS), new ObjectiveFunction(objectiveFunction),
                SimpleBounds.unbounded(2));
        long executionTime = System.nanoTime() - start;

        printOptimizationResult(objectiveFunction, bobyQAResult.getPoint(), modelData);

        return executionTime;
    }

    private void printOptimizationResult(MultivariateFunctionMappingAdapter adapter, double[] unbounded,
                                         ModelData modelData) {

        double[] boundedResult = adapter.unboundedToBounded(unbounded);
        DoubleExponentialSmoothing bestModel = DoubleExponentialSmoothing.createCustom(boundedResult[0], boundedResult[1],
                ImmutableMetricContext.getDefault());
        bestModel.init(modelData.getData());

        System.out.println(bestModel.initStatistics());
    }

    private long executePowell(ModelData modelData) {
        long start = System.nanoTime();
        PowellOptimizer optimizer = new PowellOptimizer(0.00001, 0.00001);
        PointValuePair unbounded = optimizer.optimize(
                GoalType.MINIMIZE, new MaxIter(MAX_ITER), new MaxEval(MAX_EVAL),
                new InitialGuess(INITIAL_GUESS),
                new ObjectiveFunction(objectiveFunction));
        long executionTime = System.nanoTime() - start;

        printOptimizationResult(objectiveFunction, unbounded.getPoint(), modelData);

        return executionTime;
    }

    private static void addData(List<DataPoint> to, List<DataPoint> from) {
        long lastTimestamp = to.isEmpty() ? 0 : to.get(to.size() - 1).getTimestamp();

        for (int i = 0; i < from.size(); i++) {
            to.add(new DataPoint(from.get(i).getValue(), ++lastTimestamp));
        }
    }
}
