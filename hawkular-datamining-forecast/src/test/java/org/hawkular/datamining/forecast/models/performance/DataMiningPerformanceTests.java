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

import java.util.function.Function;

import org.hawkular.datamining.forecast.MetricContext;
import org.hawkular.datamining.forecast.ModelData;
import org.hawkular.datamining.forecast.models.DoubleExponentialSmoothing;
import org.hawkular.datamining.forecast.models.ModelOptimizer;
import org.hawkular.datamining.forecast.models.SimpleExponentialSmoothing;
import org.hawkular.datamining.forecast.models.TripleExponentialSmoothing;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
@Ignore
public class DataMiningPerformanceTests extends AbstractPerformanceTest {

    @Test
    public void testPerformance() {
        testCaseAverageExecutionTime(5, modelData, metricContext, SimpleExponentialSmoothing::optimizer,
                SimpleExponentialSmoothing.class.getName());
        testCaseAverageExecutionTime(5, modelData, metricContext, DoubleExponentialSmoothing::optimizer,
                DoubleExponentialSmoothing.class.getName());
        testCaseAverageExecutionTime(5, modelData, metricContext, TripleExponentialSmoothing::optimizer,
                TripleExponentialSmoothing.class.getName());
    }

    public void testCaseAverageExecutionTime(int numberOfExecutions, ModelData modelData, MetricContext metricContext,
                                             Function<MetricContext, ModelOptimizer> modelToOptimize, String testName) {

        double sum = 0;
        for (int i = 0; i < numberOfExecutions; i++) {
            sum += findBestFit(modelData, metricContext, modelToOptimize);
        }
        System.out.println(testName + ", number of observations = " + modelData.getData().size());
        System.out.println("Data Mining, number of executions = " + numberOfExecutions +
                ", average execution time = " + (sum/numberOfExecutions)/1e9);
    }

    private long findBestFit(ModelData modelData, MetricContext metricContext,
                             Function<MetricContext, ModelOptimizer> modelOptimizer) {

        long start = System.nanoTime();
        modelOptimizer.apply(metricContext).minimizedMSE(modelData.getData());
        return System.nanoTime() - start;
    }
}
