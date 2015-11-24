/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.hawkular.datamining.api.model.DataPoint;
import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class LeastMeanSquaresFilterTest {

    /**
     * Test for AR(2) process = 1.75 0.8745
     *
     * Also checks convergence to zero - series mean
     *
     * Series generated in Matlab with
     * rand('twister', sum(100 * clock))
     * ni = rand(1, 10000) - 0.5;
     * a = [1 1.75 0.8745];
     * b = 1;
     * x = filter(b, a, ni);
     * x = x(:);
     */
    @Test
    public void testSeries() throws IOException {

        String header = "label";
        String file = this.getClass().getClassLoader().getResource("ar2.csv").getPath();

        Reader in = new FileReader(file);
        Iterable<CSVRecord> records = CSVFormat.DEFAULT.withAllowMissingColumnNames(true)
                .withHeader(header)
                .parse(in);

        List<DataPoint> dataPoints = new ArrayList<>();
        for (CSVRecord record : records) {
            String value = record.get(0);
//            record.

            Double doubleValue = null;
            try {
                doubleValue = Double.parseDouble(value);
            } catch (NumberFormatException ex) {
                continue;
            }

            DataPoint dataPoint = new DataPoint(doubleValue, 1L);
            dataPoints.add(dataPoint);
        }


        double learningRate = 0.005;
        double[] initWeights = new double[] {3, -1};

        LeastMeanSquaresFilter leastMeanSquaresFilter = new LeastMeanSquaresFilter(learningRate, initWeights);
        leastMeanSquaresFilter.addDataPoints(dataPoints);

        //AR = 1.75 0.8745
        double[] weights = leastMeanSquaresFilter.getWeights();
        double expectedAr1 = 1.75;
        double expectedAr2 = 0.8745;
        double actualAr1 = weights[0];
        double actualAr2 = weights[1];
        double maxError = 0.1;

        assertThat(actualAr1).isBetween(expectedAr1 - maxError, expectedAr1 + maxError);
        assertThat(actualAr2).isBetween(expectedAr2 - maxError, expectedAr2 + maxError);


        /**
         * Check convergence to zero
         */
        List<DataPoint> predictions = leastMeanSquaresFilter.predict(150);
        maxError = 0.01;
        double expectedlastPrediction = 0;
        DataPoint lastPredictedPoint = predictions.get(predictions.size() - 1);

        assertThat(lastPredictedPoint.getValue()).isBetween(expectedlastPrediction - maxError,
                expectedlastPrediction + maxError);
    }
}
