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

import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultivariateFunctionMappingAdapter;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.NelderMeadSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.hawkular.datamining.forecast.MetricContext;

/**
 * @author Pavol Loffay
 */
public abstract class AbstractModelOptimizer implements ModelOptimizer {
    private static final int MAX_ITER = 10000;
    private static final int MAX_EVAL = 10000;

    private MetricContext metricContext;
    protected double[] result;


    public AbstractModelOptimizer(MetricContext metricContext) {
        this.metricContext = metricContext;
    }

    public MetricContext getMetricContext() {
        return metricContext;
    }

    @Override
    public double[] result() {
        return Arrays.copyOf(result, result.length);
    }

    protected void optimize(double[] initialGuess, MultivariateFunctionMappingAdapter costFunction) {

        // Nelder-Mead Simplex
        SimplexOptimizer optimizer = new SimplexOptimizer(0.0001, 0.0001);
        PointValuePair unBoundedResult = optimizer.optimize(
                GoalType.MINIMIZE, new MaxIter(MAX_ITER), new MaxEval(MAX_EVAL),
                new InitialGuess(initialGuess),
                new ObjectiveFunction(costFunction),
                new NelderMeadSimplex(initialGuess.length));

        result = costFunction.unboundedToBounded(unBoundedResult.getPoint());
    }
}
