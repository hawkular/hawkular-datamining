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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.ModelData;
import org.junit.Ignore;
import org.junit.Test;

import net.sourceforge.openforecast.DataSet;
import net.sourceforge.openforecast.ForecastingModel;
import net.sourceforge.openforecast.Observation;
import net.sourceforge.openforecast.models.DoubleExponentialSmoothingModel;
import net.sourceforge.openforecast.models.SimpleExponentialSmoothingModel;
import net.sourceforge.openforecast.models.TripleExponentialSmoothingModel;

/**
 * @author Pavol Loffay
 */
@Ignore
public class OpenForecastPerformanceTest extends AbstractPerformanceTest {

    private DataSet dataSetSineMediumLength;
    {
        List<net.sourceforge.openforecast.DataPoint> observations = dataPointsToObservations(modelData);
        dataSetSineMediumLength = new DataSet(modelData.getName(), modelData.getPeriods(), observations);
    }

    @Test
    public void testPerformance() {
        testCaseAverageExecutionTime(5, dataSetSineMediumLength, SimpleExponentialSmoothingModel::getBestFitModel,
                SimpleExponentialSmoothingModel.class.getName());
        testCaseAverageExecutionTime(5, dataSetSineMediumLength, DoubleExponentialSmoothingModel::getBestFitModel,
                DoubleExponentialSmoothingModel.class.getName());
        testCaseAverageExecutionTime(5, dataSetSineMediumLength, TripleExponentialSmoothingModel::getBestFitModel,
                TripleExponentialSmoothingModel.class.getName());
    }

    public void testCaseAverageExecutionTime(int numberOfExecutions, DataSet dataSet,
                                             Function<DataSet, ForecastingModel> modelToOptimize, String testName) {

        double sum = 0;
        for (int i = 0; i < numberOfExecutions; i++) {
            sum += openForecastFindBestFit(dataSet, modelToOptimize);
        }
        System.out.println(testName + ", number of observations = " + dataSet.size());
        System.out.println("OpenForecast, number of executions = " + numberOfExecutions +
                ", average execution time = " + (sum/numberOfExecutions)/1e9);
    }

    private long openForecastFindBestFit(DataSet dataSet, Function<DataSet, ForecastingModel> modelToOptimize) {

        long start = System.nanoTime();
        modelToOptimize.apply(dataSet);
        // produces NPE
//        ForecastingModel openModel = Forecaster.getBestForecast(dataSetSineMediumLength);
        DoubleExponentialSmoothingModel.getBestFitModel(dataSetSineMediumLength);
        return System.nanoTime() - start;
    }

    public List<net.sourceforge.openforecast.DataPoint> dataPointsToObservations(ModelData modelData) {

        List<net.sourceforge.openforecast.DataPoint> points = new ArrayList<>();

        int counter = 1;
        for (DataPoint dataPoint: modelData.getData()) {
            Observation observation = new Observation(dataPoint.getValue());
            observation.setIndependentValue(modelData.getName(), counter++);

            points.add(observation);
        }

        return points;
    }
}
