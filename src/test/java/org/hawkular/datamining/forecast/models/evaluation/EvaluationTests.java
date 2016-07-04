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

package org.hawkular.datamining.forecast.models.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hawkular.datamining.forecast.AbstractTest;
import org.hawkular.datamining.forecast.DataPoint;
import org.hawkular.datamining.forecast.ModelData;
import org.hawkular.datamining.forecast.ModelReader;
import org.hawkular.datamining.forecast.models.DoubleExponentialSmoothing;
import org.hawkular.datamining.forecast.models.ModelOptimizer;
import org.hawkular.datamining.forecast.models.SimpleExponentialSmoothing;
import org.hawkular.datamining.forecast.models.TimeSeriesModel;
import org.hawkular.datamining.forecast.models.TripleExponentialSmoothing;
import org.hawkular.datamining.forecast.stats.AccuracyStatistics;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
@Ignore
public class EvaluationTests extends AbstractTest{

    private static final int trainSize = 200;
    private static final int testSize = 12;
    private static final int forecastingHorizon = 6;
    private List<ModelData> testModelData;
    {
        try {
            testModelData = Arrays.asList(
                    ModelReader.read("wnLowVariance"),
                    ModelReader.read("trendStatUpwardLowVar"),
                    ModelReader.read("trendStatDownwardHighVar"),
                    ModelReader.read("sineLowVarLong"),
                    ModelReader.read("sineTrendLowVar")
            );
        } catch (IOException ex) {
        }
    }

    @Test
    public void simpleEx() throws IOException {
        for (ModelData modelData: testModelData) {
            test(modelData, SimpleExponentialSmoothing.optimizer(), trainSize, testSize, forecastingHorizon);
        }
    }

    @Test
    public void doubleEx() throws IOException {
        for (ModelData modelData: testModelData) {
            test(modelData, DoubleExponentialSmoothing.optimizer(), trainSize, testSize, forecastingHorizon);
        }
    }

    @Test
    public void tripleEx() throws IOException {
        List<ModelData> modelDataList = Arrays.asList(
                ModelReader.read("sineLowVarLong"),
                ModelReader.read("sineTrendLowVar"));

        List<AccuracyStatistics> results = new ArrayList<>();

        for (ModelData modelData: modelDataList) {
            results.add(test(modelData, TripleExponentialSmoothing.optimizer(modelData.getPeriods()), trainSize, testSize,
                    forecastingHorizon));
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (AccuracyStatistics statistics: results) {
            if (stringBuilder.length() != 0) {
                stringBuilder.append(" & ");
            }
            stringBuilder.append(String.format("%.2f", statistics.getMse()));
        }

        System.out.println("\nResult for LaTeX: " + stringBuilder);
    }

    private AccuracyStatistics test(ModelData data, ModelOptimizer optimizer, int trainSize,
                                    int numberOfPredictions, int forecastingHorizon) {

        List<DataPoint> trainData = data.getData().subList(0, trainSize);
        TimeSeriesModel model = optimizer.minimizedMSE(trainData);

        double sse = 0;
        double absSum = 0;
        for (int i = 0; i < numberOfPredictions; i++) {
            double forecast  = model.forecast(forecastingHorizon).get(forecastingHorizon - 1).getValue();
            double error = data.getData().get(trainSize + i + forecastingHorizon -1).getValue() - forecast;

//            System.out.println(error);
//            System.out.println("Original index =" + data.getData().get(trainSize + i + forecastingHorizon -1));

            sse += error*error;
            absSum += Math.abs(error);

            model.learn(data.getData().get(trainSize + i));
        }

        AccuracyStatistics accuracyStatistics = new AccuracyStatistics(sse, sse/numberOfPredictions,
                absSum/numberOfPredictions);

        /**
         * Print result
         */
        printResult(data, accuracyStatistics, trainSize, numberOfPredictions, forecastingHorizon);
//        System.out.println(model + "\n");

        return accuracyStatistics;
    }

    private void printResult(ModelData modelData, AccuracyStatistics accuracyStatistics, int trainSize,
                             int numberOfPredictions, int forecastingHorizon) {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("\n");
        stringBuilder.append(modelData.getName()).append("\n");
        stringBuilder.append("trainSize= ").append(trainSize).append(", end= ").append(trainSize+numberOfPredictions)
                .append("\n");
        stringBuilder.append("Total number of predictions= ").append(numberOfPredictions).append("\n");
        stringBuilder.append("Horizon= ").append(forecastingHorizon).append("\n");
        stringBuilder.append("Statistics ").append(accuracyStatistics);

        System.out.println(stringBuilder);
    }
}
